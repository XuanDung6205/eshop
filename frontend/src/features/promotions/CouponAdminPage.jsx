import { useEffect, useMemo, useRef, useState } from 'react';
import { useCommerce } from '../../lib/session';
import { formatDate, formatMoney, toLocalInput, toUtc } from '../../lib/format';
import { ErrorNotice, Icon, Loading, Pager } from '../../components/ui';
import { couponApi } from './couponApi';

const freshForm = () => ({ code: '', name: '', discountType: 'PERCENT', discountValue: '', maxDiscountAmount: '', minimumOrderAmount: '0.00', totalUsageLimit: '', perUserLimit: '', startsAt: '', endsAt: '', isActive: true });
const couponToForm = coupon => ({ ...Object.fromEntries(Object.keys(freshForm()).map(key => [key, coupon[key] ?? ''])), startsAt: toLocalInput(coupon.startsAt), endsAt: toLocalInput(coupon.endsAt) });

function CouponForm({ coupon, client, onSaved, onCancel }) {
  const [form, setForm] = useState(() => coupon ? couponToForm(coupon) : freshForm());
  const [error, setError] = useState(null);
  const [fields, setFields] = useState({});
  const [busy, setBusy] = useState(false);
  const errorRef = useRef(null);
  const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  useEffect(() => { if (error) errorRef.current?.focus(); }, [error]);
  function change(key, value) { setForm(current => ({ ...current, [key]: value })); setFields(current => ({ ...current, [key]: undefined })); }
  function field(key, label, options = {}) {
    return <div className={`form-field ${options.wide ? 'wide' : ''}`}><label htmlFor={`coupon-${key}`}>{label}</label><input id={`coupon-${key}`} name={key} value={form[key]} onChange={event => change(key, event.target.value)} disabled={busy} aria-invalid={!!fields[key]} aria-describedby={fields[key] ? `error-${key}` : undefined} {...Object.fromEntries(Object.entries(options).filter(([key]) => key !== 'wide'))} />{fields[key] && <span id={`error-${key}`} className="field-error">{fields[key]}</span>}</div>;
  }
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError(null); setFields({});
    try {
      const body = { ...form, code: form.code.trim().toUpperCase(), name: form.name.trim() || null, maxDiscountAmount: form.maxDiscountAmount || null, totalUsageLimit: form.totalUsageLimit === '' ? null : Number(form.totalUsageLimit), perUserLimit: form.perUserLimit === '' ? null : Number(form.perUserLimit), startsAt: toUtc(form.startsAt, coupon?.startsAt), endsAt: toUtc(form.endsAt, coupon?.endsAt) };
      const result = coupon ? await client.update(coupon.id, body) : await client.create(body);
      onSaved(result);
    } catch (value) { setError(value); setFields(value.fieldErrors || {}); }
    finally { setBusy(false); }
  }
  const decimal = { inputMode: 'decimal', pattern: '[0-9]+([.][0-9]{1,2})?', title: 'Dùng dấu chấm thập phân, tối đa 2 chữ số sau dấu chấm.' };
  return <section className="panel coupon-editor" aria-label={coupon ? `Sửa mã ${coupon.code}` : 'Tạo mã mới'}><div className="panel-heading"><div><span className="eyebrow">THÔNG TIN ƯU ĐÃI</span><h2>{coupon ? `Chỉnh sửa ${coupon.code}` : 'Tạo mã ưu đãi'}</h2></div><button className="icon-button" aria-label="Đóng trình chỉnh sửa" disabled={busy} onClick={onCancel}><Icon name="close" /></button></div>
    <form aria-label="Thông tin mã ưu đãi" onSubmit={submit}><div ref={errorRef} tabIndex="-1"><ErrorNotice error={error} /></div><div className="form-grid">
      {field('code', 'Mã ưu đãi', { required: true, maxLength: 50, pattern: '[A-Za-z0-9_-]+', autoFocus: true, placeholder: 'VD: TOAM10' })}
      {field('name', 'Tên chương trình', { maxLength: 150, placeholder: 'Tên gợi nhớ cho chương trình' })}
      <div className="form-field"><label htmlFor="coupon-discountType">Hình thức giảm</label><select id="coupon-discountType" value={form.discountType} onChange={event => change('discountType', event.target.value)} disabled={busy}><option value="PERCENT">Theo phần trăm (%)</option><option value="FIXED">Số tiền cố định (₫)</option></select>{fields.discountType && <span className="field-error">{fields.discountType}</span>}</div>
      {field('discountValue', form.discountType === 'PERCENT' ? 'Giá trị giảm (%)' : 'Giá trị giảm (₫)', { ...decimal, required: true })}
      {field('maxDiscountAmount', 'Giảm tối đa (₫)', { ...decimal, placeholder: 'Không giới hạn' })}
      {field('minimumOrderAmount', 'Tiền hàng tối thiểu (₫)', { ...decimal, required: true })}
      {field('totalUsageLimit', 'Tổng lượt sử dụng', { type: 'number', min: 1, max: 2147483647, step: 1, placeholder: 'Không giới hạn' })}
      {field('perUserLimit', 'Lượt mỗi tài khoản', { type: 'number', min: 1, max: 2147483647, step: 1, placeholder: 'Không giới hạn' })}
      {field('startsAt', 'Bắt đầu', { type: 'datetime-local', step: '0.001', required: true })}
      {field('endsAt', 'Kết thúc', { type: 'datetime-local', step: '0.001', required: true })}
      <p className="form-hint wide">Thời gian theo múi giờ {timezone}. Hạn kết thúc không bao gồm chính thời điểm đó. Các ô giới hạn để trống nghĩa là không giới hạn.</p>
      <label className="checkbox-label wide"><input type="checkbox" checked={form.isActive} disabled={busy} onChange={event => change('isActive', event.target.checked)} />Cho phép sử dụng mã</label>
    </div><div className="editor-actions"><button type="button" className="button secondary" disabled={busy} onClick={onCancel}>Hủy</button><button className="button" disabled={busy}>{busy ? 'Đang lưu…' : coupon ? 'Lưu thay đổi' : 'Tạo mã ưu đãi'}<Icon name="arrow" size={18} /></button></div></form>
  </section>;
}

function CouponPreview({ coupon, client, onClose }) {
  const [amount, setAmount] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError(null); setResult(null);
    try { setResult(await client.preview(coupon.id, amount)); }
    catch (value) { setError(value); }
    finally { setBusy(false); }
  }
  return <section className="panel preview-panel"><div className="panel-heading"><div><span className="eyebrow">TÍNH THỬ ƯU ĐÃI</span><h2>{coupon.code}</h2></div><button className="icon-button" onClick={onClose} disabled={busy} aria-label="Đóng xem thử"><Icon name="close" /></button></div><p className="muted">Cơ sở là tiền hàng sau giảm theo dòng, chưa gồm phí giao, lắp đặt và thuế. Kết quả không giữ lượt sử dụng mã.</p><form onSubmit={submit}><label htmlFor="preview-amount">Tiền hàng để tính thử (₫)</label><div className="preview-input"><input id="preview-amount" required inputMode="decimal" pattern="[0-9]+([.][0-9]{1,2})?" value={amount} onChange={event => { setAmount(event.target.value); setResult(null); }} disabled={busy} aria-invalid={!!error?.fieldErrors?.merchandiseAmount} /><button className="button" disabled={busy}>{busy ? 'Đang tính…' : 'Tính ưu đãi'}</button></div>{error?.fieldErrors?.merchandiseAmount && <span className="field-error">{error.fieldErrors.merchandiseAmount}</span>}</form><ErrorNotice error={error} />{result && <dl className="preview-result" role="status"><div><dt>Được giảm</dt><dd>{formatMoney(result.discountAmount)}</dd></div><div><dt>Tiền hàng còn lại</dt><dd>{formatMoney(result.remainingAmount)}</dd></div></dl>}</section>;
}

export default function CouponAdminPage() {
  const { api } = useCommerce();
  const client = useMemo(() => couponApi(api), [api]);
  const [data, setData] = useState(null);
  const [query, setQuery] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [revision, setRevision] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState('');
  const [editing, setEditing] = useState(undefined);
  const [preview, setPreview] = useState(null);
  const [deleting, setDeleting] = useState(null);
  useEffect(() => {
    const controller = new AbortController(); setLoading(true); setError(null);
    client.list(query, page, controller.signal).then(value => { setData(value); if (!value.items.length && page > 0) setPage(page - 1); }).catch(value => { if (value.name !== 'AbortError') setError(value); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [client, page, query, revision]);
  async function action(callback, message) {
    if (busy) return;
    setBusy(true); setError(null); setNotice('');
    try { await callback(); setNotice(message); setRevision(value => value + 1); setDeleting(null); setPreview(null); }
    catch (value) { setError(value); if (value.code === 'COUPON_IN_USE') setDeleting(null); }
    finally { setBusy(false); }
  }
  return <>
    <div className="breadcrumb"><span>Quản trị</span><span>/</span><span>Ưu đãi</span></div>
    <div className="page-heading"><div><span className="eyebrow">KHÔNG GIAN QUẢN TRỊ</span><h1>Quản lý ưu đãi<span className="heading-dot">.</span></h1><p>Những ưu đãi nhỏ, thêm cảm hứng cho mỗi tổ ấm.</p></div><button className="button" onClick={() => { setEditing(null); setPreview(null); }} disabled={busy || editing !== undefined}><Icon name="plus" size={18} />Tạo mã mới</button></div>
    <ErrorNotice error={error} onRetry={() => setRevision(value => value + 1)} />
    {notice && <div className="notice success" role="status">{notice}</div>}
    {editing !== undefined && <CouponForm key={editing?.id ?? 'new'} coupon={editing} client={client} onCancel={() => setEditing(undefined)} onSaved={saved => { setEditing(undefined); setNotice(`Đã lưu mã ${saved.code}.`); setRevision(value => value + 1); }} />}
    {preview && <CouponPreview key={preview.id} coupon={preview} client={client} onClose={() => setPreview(null)} />}
    <section className="panel coupons-panel" aria-label="Danh sách mã ưu đãi"><div className="panel-heading coupon-toolbar"><div><h2>Tất cả mã ưu đãi <span className="count-badge">{data?.totalElements ?? '—'}</span></h2><p className="muted small">Theo dõi và điều chỉnh chương trình của cửa hàng.</p></div><form className="search-form" onSubmit={event => { event.preventDefault(); setPage(0); setQuery(search.trim()); }}><Icon name="search" /><label className="sr-only" htmlFor="coupon-search">Tìm mã ưu đãi</label><input type="search" id="coupon-search" value={search} onChange={event => setSearch(event.target.value)} placeholder="Tìm theo mã hoặc tên" /><button className="button secondary" disabled={loading || busy}>Tìm kiếm</button></form></div>
      {loading ? <Loading text="Đang tải mã ưu đãi…" /> : data?.items.length ? <div className="coupon-list"><div className="coupon-column-head" aria-hidden="true"><span>MÃ / CHƯƠNG TRÌNH</span><span>ƯU ĐÃI</span><span>THỜI GIAN ÁP DỤNG</span><span>TRẠNG THÁI</span><span>THAO TÁC</span></div>{data.items.map(coupon => <article key={coupon.id} className="coupon-row"><div className="coupon-name"><div className="coupon-symbol"><Icon name="ticket" size={24} /></div><div><h3>{coupon.code}</h3><p>{coupon.name || 'Chưa đặt tên chương trình'}</p></div></div><div className="coupon-value"><strong>{coupon.discountType === 'PERCENT' ? `${coupon.discountValue}%` : formatMoney(coupon.discountValue)}</strong><small>Đơn từ {formatMoney(coupon.minimumOrderAmount)}</small>{coupon.maxDiscountAmount && <small>Tối đa {formatMoney(coupon.maxDiscountAmount)}</small>}</div><div className="coupon-dates"><span>{formatDate(coupon.startsAt)}</span><span>đến {formatDate(coupon.endsAt)}</span></div><div><span className={`status-pill ${coupon.isActive ? 'active' : 'inactive'}`}><span />{coupon.isActive ? 'Đang bật' : 'Đã tắt'}</span></div><div className="coupon-actions"><button className="text-button" disabled={busy || editing !== undefined} aria-label={`Sửa ${coupon.code}`} onClick={() => { setEditing(coupon); setPreview(null); }}>Sửa</button><button className="text-button" disabled={busy || editing !== undefined} aria-label={`${coupon.isActive ? 'Tắt' : 'Bật'} ${coupon.code}`} onClick={() => action(() => client.update(coupon.id, { isActive: !coupon.isActive }), `Đã ${coupon.isActive ? 'tắt' : 'bật'} mã ${coupon.code}.`)}>{coupon.isActive ? 'Tắt' : 'Bật'}</button><button className="text-button" disabled={busy} aria-label={`Xem thử ${coupon.code}`} onClick={() => { setPreview(coupon); setEditing(undefined); }}>Xem thử</button><button className="text-button destructive" disabled={busy || editing !== undefined} aria-label={`Xóa ${coupon.code}`} onClick={() => setDeleting(coupon)}>Xóa</button></div></article>)}</div> : !error && <div className="empty-state"><Icon name="ticket" size={38} /><h3>Chưa có mã ưu đãi phù hợp</h3><p>Tạo chương trình mới hoặc thử từ khóa khác.</p></div>}
      {deleting && <div className="notice warning delete-confirm" role="alert"><div><strong>Xóa mã {deleting.code}?</strong><p>Không thể hoàn tác. Mã đã được tham chiếu chỉ có thể tắt.</p></div><button className="button secondary" disabled={busy} onClick={() => setDeleting(null)}>Giữ lại</button><button className="button danger" disabled={busy} onClick={() => action(() => client.remove(deleting.id), `Đã xóa mã ${deleting.code}.`)}>{busy ? 'Đang xóa…' : 'Xác nhận xóa'}</button></div>}
      <Pager page={data} busy={loading || busy} onChange={setPage} />
    </section><p className="admin-footnote"><Icon name="shield" size={18} />Mã đã được sử dụng được bảo toàn lịch sử. Tắt mã để ngừng các lượt sử dụng mới.</p>
  </>;
}
