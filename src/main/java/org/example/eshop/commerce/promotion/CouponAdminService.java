package org.example.eshop.commerce.promotion;

import org.example.eshop.commerce.common.CommerceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class CouponAdminService {
    private static final Set<String> FIELDS=Set.of("code","name","discountType","discountValue","maxDiscountAmount","minimumOrderAmount","totalUsageLimit","perUserLimit","startsAt","endsAt","isActive");
    private final CouponRepository repository;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final CouponCalculator calculator;
    public CouponAdminService(CouponRepository repository,JdbcTemplate jdbc,Clock clock,CouponCalculator calculator){this.repository=repository;this.jdbc=jdbc;this.clock=clock;this.calculator=calculator;}
    public record CouponPage(List<CouponView> items,int page,int size,long totalElements,int totalPages){}
    public record Preview(String discountAmount,String remainingAmount,String currency){}

    @Transactional(readOnly=true)
    public CouponPage list(String query,int page,int size){
        if(page<0 || size<1 || size>100 || (query!=null && query.length()>150)) throw CommerceException.badRequest("INVALID_PAGINATION","Trang, số dòng hoặc từ khóa không hợp lệ.");
        var results=repository.search(query==null?"":query.trim().toLowerCase(Locale.ROOT),PageRequest.of(page,size,Sort.by("id").descending()));
        return new CouponPage(results.getContent().stream().map(CouponView::from).toList(),page,size,results.getTotalElements(),results.getTotalPages());
    }
    @Transactional(readOnly=true) public CouponView get(Long id){return CouponView.from(find(id));}
    @Transactional public CouponView create(JsonNode body){
        CouponEntity c=new CouponEntity(); apply(c,body,true); LocalDateTime now=now();c.setCreatedAt(now);c.setUpdatedAt(now);
        return save(c);
    }
    @Transactional public CouponView patch(Long id,JsonNode body){
        CouponEntity c=repository.lockById(id).orElseThrow(()->CommerceException.notFound("Không tìm thấy mã giảm giá."));
        String previousCode=c.getCode();apply(c,body,false);
        if(!previousCode.equals(c.getCode()) && used(id)) throw inUse();
        c.setUpdatedAt(now());return save(c);
    }
    @Transactional public void delete(Long id){
        CouponEntity c=repository.lockById(id).orElseThrow(()->CommerceException.notFound("Không tìm thấy mã giảm giá."));
        if(used(id)) throw inUse();
        try {repository.delete(c);repository.flush();} catch(DataIntegrityViolationException e){throw inUse();}
    }
    @Transactional(readOnly=true) public Preview preview(Long id,JsonNode body){
        if(body==null || !body.isObject() || body.size()!=1 || !body.has("merchandiseAmount")) throw CommerceException.badRequest("INVALID_REQUEST","Cần nhập merchandiseAmount để tính thử.");
        Map<String,String> errors=new LinkedHashMap<>();BigDecimal base=amount(body,"merchandiseAmount",true,false,errors);
        if(!errors.isEmpty()) throw CommerceException.invalid(errors);
        BigDecimal discount=calculator.discount(find(id),base);
        return new Preview(CouponView.money(discount),CouponView.money(base.subtract(discount)),"VND");
    }
    private CouponEntity find(Long id){return repository.findById(id).orElseThrow(()->CommerceException.notFound("Không tìm thấy mã giảm giá."));}
    private boolean used(Long id){return jdbc.queryForObject("SELECT (SELECT COUNT(*) FROM orders WHERE coupon_id=?) + (SELECT COUNT(*) FROM coupon_redemptions WHERE coupon_id=?)",Long.class,id,id)>0;}
    private CommerceException inUse(){return CommerceException.conflict("COUPON_IN_USE","Mã đã có lịch sử sử dụng. Hãy ngừng sử dụng thay vì xóa hoặc đổi mã.");}
    private LocalDateTime now(){return LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC);}
    private CouponView save(CouponEntity c){try{return CouponView.from(repository.saveAndFlush(c));}catch(DataIntegrityViolationException e){throw CommerceException.conflict("COUPON_CODE_EXISTS","Mã giảm giá đã tồn tại hoặc không đáp ứng ràng buộc dữ liệu.");}}
    private void apply(CouponEntity c,JsonNode body,boolean create){
        if(body==null || !body.isObject() || body.isEmpty()) throw CommerceException.badRequest("INVALID_REQUEST","Nội dung yêu cầu không hợp lệ.");
        Map<String,String> errors=new LinkedHashMap<>();
        body.propertyNames().forEach(key->{if(!FIELDS.contains(key))errors.put(key,"Thuộc tính không được phép cập nhật.");});
        if(create || body.has("code")) {String code=text(body,"code",true,50,errors);if(code!=null){code=code.trim().toUpperCase(Locale.ROOT);if(!code.matches("[A-Z0-9_-]{1,50}"))errors.put("code","Mã chỉ gồm chữ, số, dấu gạch ngang hoặc gạch dưới.");c.setCode(code);}}
        if(body.has("name")) c.setName(text(body,"name",false,150,errors));
        if(create || body.has("discountType")){String type=text(body,"discountType",true,20,errors);if(!Set.of("PERCENT","FIXED").contains(type==null?"":type))errors.put("discountType","Chọn PERCENT hoặc FIXED.");else c.setDiscountType(type);}
        if(create || body.has("discountValue")) c.setDiscountValue(amount(body,"discountValue",true,true,errors));
        if(body.has("maxDiscountAmount")) c.setMaxDiscountAmount(amount(body,"maxDiscountAmount",false,true,errors));
        if(body.has("minimumOrderAmount")) c.setMinimumOrderAmount(amount(body,"minimumOrderAmount",true,false,errors));
        if(body.has("totalUsageLimit")) c.setTotalUsageLimit(limit(body,"totalUsageLimit",errors));
        if(body.has("perUserLimit")) c.setPerUserLimit(limit(body,"perUserLimit",errors));
        if(create || body.has("startsAt")) c.setStartsAt(date(body,"startsAt",errors));
        if(create || body.has("endsAt")) c.setEndsAt(date(body,"endsAt",errors));
        if(body.has("isActive")){JsonNode n=body.get("isActive");if(!n.isBoolean())errors.put("isActive","Cần giá trị true hoặc false.");else c.setIsActive(n.asBoolean());}
        if(c.getMinimumOrderAmount()==null)errors.put("minimumOrderAmount","Giá trị không được trống.");
        if(c.getDiscountValue()!=null && "PERCENT".equals(c.getDiscountType()) && c.getDiscountValue().compareTo(new BigDecimal("100"))>0)errors.put("discountValue","Phần trăm giảm không vượt 100.");
        if(c.getStartsAt()!=null && c.getEndsAt()!=null && !c.getEndsAt().isAfter(c.getStartsAt()))errors.put("endsAt","Ngày kết thúc phải sau ngày bắt đầu.");
        if(!errors.isEmpty())throw CommerceException.invalid(errors);
    }
    private String text(JsonNode body,String field,boolean required,int max,Map<String,String> errors){
        JsonNode n=body.get(field);if(n==null || n.isNull()){if(required)errors.put(field,"Thông tin bắt buộc.");return null;}
        if(!n.isTextual()){errors.put(field,"Cần nhập chuỗi ký tự.");return null;}
        String value=n.asText().trim();if((required && value.isEmpty()) || value.length()>max)errors.put(field,"Thông tin trống hoặc vượt "+max+" ký tự.");return value.isEmpty()?null:value;
    }
    private BigDecimal amount(JsonNode body,String field,boolean required,boolean positive,Map<String,String> errors){
        String value=text(body,field,required,32,errors);if(value==null)return null;
        if(!value.matches("(0|[1-9][0-9]{0,12})(\\.[0-9]{1,2})?")){errors.put(field,"Số tiền không âm, tối đa 13 chữ số nguyên và 2 chữ số thập phân; gửi dưới dạng chuỗi.");return null;}
        BigDecimal parsed=new BigDecimal(value).setScale(2);if(positive && parsed.signum()<=0)errors.put(field,"Giá trị phải lớn hơn 0.");return parsed;
    }
    private Integer limit(JsonNode body,String field,Map<String,String> errors){JsonNode n=body.get(field);if(n==null || n.isNull())return null;if(!n.isIntegralNumber() || !n.canConvertToInt() || n.asInt()<=0){errors.put(field,"Giới hạn phải là số nguyên dương hoặc để trống.");return null;}return n.asInt();}
    private LocalDateTime date(JsonNode body,String field,Map<String,String> errors){String value=text(body,field,true,80,errors);if(value==null)return null;try{return LocalDateTime.ofInstant(Instant.parse(value),ZoneOffset.UTC);}catch(RuntimeException e){errors.put(field,"Ngày cần theo ISO-8601 có múi giờ, ví dụ 2026-11-13T10:00:00Z.");return null;}}
}
