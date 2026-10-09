package com.mycompany.myapp.service.dvhc;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.service.dvhc.ListingLinkPlanner.Context;
import com.mycompany.myapp.service.dvhc.ListingLinkPlanner.Decision;
import com.mycompany.myapp.service.dvhc.ListingLinkPlanner.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListingLinkPlannerTest {

    // Bộ cũ: Hà Nội (2) > Ba Đình (001, id 20) > Thành Công (00175, id 793), Giảng Võ (00178, id 792); Đống Đa (006, id 30) > Láng Hạ (00229, id 849)
    //        Bắc Giang (24, id 16) > Huyện Yên Dũng (221, id 40) > Xã X (07000, id 900)
    //        QUỐC TẾ (id 1, không mã)
    // Bộ mới: Hà Nội (01, id 11960) > Giảng Võ (00025, id 11998), Láng (00199, id 12005); Bắc Ninh (24, id 11963) > Xã Y (07100, id 13000)
    private Context ctx;

    @BeforeEach
    void setUp() {
        ctx = new Context();
        ctx.addUnit(new Unit(2, false, "province", "01", "01"));
        ctx.addUnit(new Unit(20, false, "district", "001", "01"));
        ctx.addUnit(new Unit(30, false, "district", "006", "01"));
        ctx.addUnit(new Unit(793, false, "ward", "00175", "001"));
        ctx.addUnit(new Unit(792, false, "ward", "00178", "001"));
        ctx.addUnit(new Unit(849, false, "ward", "00229", "006"));
        ctx.addUnit(new Unit(16, false, "province", "24", "24"));
        ctx.addUnit(new Unit(40, false, "district", "221", "24"));
        ctx.addUnit(new Unit(900, false, "ward", "07000", "221"));
        ctx.addUnit(new Unit(1, false, "province", null, null));
        ctx.addUnit(new Unit(11960, true, "province", "01", "01"));
        ctx.addUnit(new Unit(11998, true, "ward", "00025", "01"));
        ctx.addUnit(new Unit(12005, true, "ward", "00199", "01"));
        ctx.addUnit(new Unit(11963, true, "province", "24", "24"));
        ctx.addUnit(new Unit(13000, true, "ward", "07100", "24"));
        // Giảng Võ cũ nhập toàn bộ vào Giảng Võ mới; Thành Công cũ chia cho Giảng Võ mới và Láng mới (nhập một phần)
        ctx.addConversion("01", "00025", "00178", "001", "01");
        ctx.addConversion("01", "00025", "00175", "001", "01");
        ctx.addConversion("01", "00199", "00175", "001", "01");
        // Láng mới gồm Láng Hạ cũ (Đống Đa) → Láng mới gộp từ 2 huyện cũ
        ctx.addConversion("01", "00199", "00229", "006", "01");
        // Xã Y mới (Bắc Ninh) gộp Xã X cũ (Bắc Giang)
        ctx.addConversion("24", "07100", "07000", "221", "24");
    }

    private Decision plan(String jsonProvince, String jsonWard, long... current) {
        return ListingLinkPlanner.plan(ctx, jsonProvince, jsonWard, current);
    }

    @Test
    void uuTienXaMoiTrongJsonVaSoKhopVoiXaCu() {
        Decision d = plan("01", "00025", 792);
        assertThat(d.add()).containsExactly(11998L);
        assertThat(d.newSource()).isEqualTo("JSON: xã mới");
        assertThat(d.jsonAgreesWithOldWard()).isTrue();
        assertThat(plan("01", "00199", 792).jsonAgreesWithOldWard()).isFalse();
    }

    @Test
    void jsonChiCoTinhThiGanTinhMoi() {
        Decision d = plan("01", null, 2);
        assertThat(d.add()).containsExactly(11960L);
        assertThat(d.newSource()).isEqualTo("JSON: tỉnh mới");
    }

    @Test
    void khongCoJsonThiSuyTuLienKetCu() {
        assertThat(plan(null, null, 792).add()).containsExactly(11998L); // xã cũ về đúng 1 xã mới
        assertThat(plan(null, null, 793).add()).containsExactly(11960L); // nhập một phần vào 2 xã cùng tỉnh → tỉnh mới
        assertThat(plan(null, null, 40).add()).containsExactly(13000L); // huyện cũ về 1 xã mới
        assertThat(plan(null, null, 16).add()).containsExactly(11963L); // Bắc Giang cũ → Bắc Ninh mới
        assertThat(plan(null, null, 2, 792).add()).containsExactly(11998L); // chọn đơn vị cụ thể nhất
    }

    @Test
    void daCoDonViMoiThiKhongThemDonViMoiNhungThemDonViCu() {
        // Chỉ gắn xã mới Giảng Võ: mọi xã cũ hợp thành thuộc huyện Ba Đình → thêm huyện cũ
        Decision d = plan(null, null, 11998);
        assertThat(d.add()).containsExactly(20L);
        assertThat(d.newSource()).isEqualTo("đã có đơn vị mới");
        assertThat(d.oldSource()).isEqualTo("thêm huyện cũ");
        // Láng mới gộp từ 2 huyện cũ (Ba Đình, Đống Đa), cùng tỉnh cũ → thêm tỉnh cũ
        assertThat(plan(null, null, 12005).add()).containsExactly(2L);
    }

    @Test
    void chuaGanGiCoJsonThiThemCaMoiLanCu() {
        Decision d = plan("24", "07100");
        assertThat(d.add()).containsExactly(13000L, 40L);
        assertThat(plan(null, null).add()).isEmpty();
        assertThat(plan(null, null).newSource()).isEqualTo("không có nguồn");
    }

    @Test
    void quocTeKhongChuyenDuoc() {
        Decision d = plan(null, null, 1);
        assertThat(d.add()).isEmpty();
        assertThat(d.newSource()).startsWith("không chuyển được");
    }

    @Test
    void docMaTuJsonKeCaJsonHong() {
        assertThat(
            ListingLinkPlanner.jsonCodes("{\"province\":\"Hà Nội\",\"ward_code\":\"00025\",\"province_code\":\"01\",\"x\":\"a\"b\"}")
        ).containsExactly("01", "00025");
        assertThat(ListingLinkPlanner.jsonCodes("{\"province_code\":\"01\",\"ward_code\":null}")).containsExactly("01", null);
        assertThat(ListingLinkPlanner.jsonCodes(null)).containsExactly(null, null);
    }
}
