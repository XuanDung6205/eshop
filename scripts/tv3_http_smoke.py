"""Exercise actual local HTTP/session/CSRF/cart/coupon paths. No third-party packages.

Requires TV3_DEMO_PASSWORD and an isolated seeded TV3 dev runtime. It does not
reset databases or delete existing coupons/cart data belonging to other sessions.
"""
import argparse
import http.cookiejar
import json
import os
import sys
import uuid
from datetime import datetime,timedelta,timezone
from urllib.error import HTTPError
from urllib.parse import urlencode,urlparse
from urllib.request import build_opener,HTTPCookieProcessor,Request

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url',default='http://127.0.0.1:8080')
    args=parser.parse_args()
    if urlparse(args.base_url).hostname not in {'localhost','127.0.0.1','::1'}:
        parser.error('This smoke tool sends credentials only to a local runtime.')
    password=os.environ.get('TV3_DEMO_PASSWORD')
    if not password:parser.error('Set TV3_DEMO_PASSWORD in environment.')
    opener=build_opener(HTTPCookieProcessor(http.cookiejar.CookieJar()))
    def call(path,method='GET',body=None,expected=200,form=False,secure=True):
        headers={'Accept':'application/json'}
        if method not in {'GET','HEAD'} and secure:
            token=call('/api/csrf');headers[token['headerName']]=token['token']
        data=None
        if body is not None:
            headers['Content-Type']='application/x-www-form-urlencoded' if form else 'application/json'
            data=(urlencode(body) if form else json.dumps(body)).encode()
        request=Request(args.base_url.rstrip('/')+path,data=data,method=method,headers=headers)
        try:
            with opener.open(request,timeout=15) as response:status,payload=response.status,response.read()
        except HTTPError as error:status,payload=error.code,error.read()
        result=json.loads(payload) if payload else None
        assert status==expected,f'{method} {path}: expected HTTP {expected}, got {status}; code={result.get("code") if isinstance(result,dict) else "non-json"}'
        return result
    runtime=call('/api/commerce/runtime')
    assert runtime.get('application')=='tv3-commerce' and runtime.get('isolatedData') is True,'Refusing mutations: runtime is not connected to an isolated TV3 demo/test database.'
    cart=call('/api/cart');assert cart['itemCount']==0
    variants=call('/api/commerce/catalog/variants?size=12')['items']
    variant=next(v for v in variants if v['availableQuantity']>=3)
    assert isinstance(variant['price'],str)
    call('/api/cart/items','POST',{'variantId':variant['id'],'quantity':1},expected=403,secure=False)
    cart=call('/api/cart/items','POST',{'variantId':variant['id'],'quantity':1})
    item=cart['items'][0];assert item['quantity']==1 and isinstance(cart['subtotal'],str)
    cart=call('/api/cart/items','POST',{'variantId':variant['id'],'quantity':1});assert len(cart['items'])==1 and cart['itemCount']==2
    cart=call('/api/cart/items/'+str(item['id']),'PATCH',{'quantity':3});assert cart['itemCount']==3
    assert call('/api/cart')['itemCount']==3
    call('/api/cart/items/'+str(item['id']),'PATCH',{'quantity':0},expected=400)
    assert call('/api/cart/items/'+str(item['id']),'DELETE')['itemCount']==0
    call('/api/admin/coupons',expected=401)
    username=os.environ.get('TV3_DEMO_USERNAME','tv3.admin')
    call('/api/session/login','POST',{'username':username,'password':'intentionally-wrong'},expected=401,form=True)
    session=call('/api/session/login','POST',{'username':username,'password':password},form=True)
    assert session['authenticated'] and 'ADMIN' in session['user']['roles']
    coupon_id=None
    try:
        now=datetime.now(timezone.utc)
        coupon=call('/api/admin/coupons','POST',{'code':'SMOKE_'+uuid.uuid4().hex[:12].upper(),'discountType':'PERCENT','discountValue':'10','minimumOrderAmount':'0','startsAt':(now-timedelta(minutes=1)).isoformat(),'endsAt':(now+timedelta(days=1)).isoformat()},expected=201)
        coupon_id=coupon['id'];assert coupon['discountValue']=='10.00'
        preview=call(f'/api/admin/coupons/{coupon_id}/preview','POST',{'merchandiseAmount':'1000.00'});assert preview['discountAmount']=='100.00'
        call(f'/api/admin/coupons/{coupon_id}','PATCH',{'isActive':False})
        call(f'/api/admin/coupons/{coupon_id}/preview','POST',{'merchandiseAmount':'1000.00'},expected=409)
        call(f'/api/admin/coupons/{coupon_id}','DELETE',expected=204);coupon_id=None
    finally:
        if coupon_id:call(f'/api/admin/coupons/{coupon_id}','DELETE',expected=204)
    assert not call('/api/session/logout','POST')['authenticated']
    call('/api/admin/coupons',expected=401)
    print('PASS: actual local HTTP guest cart persistence/mutations, CSRF, real login/logout, role checks, coupon CRUD/preview; created coupon cleaned up.')

if __name__=='__main__':
    try:main()
    except Exception as error:print('FAIL: '+str(error),file=sys.stderr);sys.exit(1)
