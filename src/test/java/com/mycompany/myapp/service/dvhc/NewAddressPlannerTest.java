package com.mycompany.myapp.service.dvhc;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.service.dvhc.NewAddressPlanner.Result;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NewAddressPlannerTest {

    private static final Set<String> PROVINCES = Set.of(
        "ha noi",
        "bac ninh",
        "bac giang",
        "ho chi minh",
        "dong nai",
        "long an",
        "tay ninh",
        "binh dinh",
        "gia lai",
        "hcm",
        "tphcm"
    );
    private static final Set<String> HN_DISTRICTS = Set.of("ba dinh", "hai ba trung", "cau giay", "thanh xuan", "nam tu liem");

    private static Result plan(String address, String ward, String province, Set<String> districts) {
        return NewAddressPlanner.plan(address, ward, province, PROVINCES, districts, Set.of());
    }

    @Test
    void giuSoNhaDuongBoPhuongQuanTinhViệtNam() {
        assertThat(
            plan(
                "Số 12, ngõ 72 Phố Tân Ấp, Phường Phúc Xá, Quận Ba Đình, Thành phố Hà Nội, Việt Nam",
                "Phường Hồng Hà",
                "Thành phố Hà Nội",
                HN_DISTRICTS
            ).addressNew()
        ).isEqualTo("Số 12, ngõ 72 Phố Tân Ấp, Phường Hồng Hà, Thành phố Hà Nội");
        // Viết tắt, chữ hoa; giữ "P3", "KHU B4B"
        assertThat(
            plan("P3, KHU B4B, P. THÀNH CÔNG, Q. BA ĐÌNH, HÀ NỘI", "Phường Giảng Võ", "Thành phố Hà Nội", HN_DISTRICTS).addressNew()
        ).isEqualTo("P3, KHU B4B, Phường Giảng Võ, Thành phố Hà Nội");
        // Tên quận không có tiền tố
        assertThat(
            plan("Tầng 3, 352 Giải Phóng, Thanh Xuân, Hà Nội", "Phường Phương Liệt", "Thành phố Hà Nội", HN_DISTRICTS).addressNew()
        ).isEqualTo("Tầng 3, 352 Giải Phóng, Phường Phương Liệt, Thành phố Hà Nội");
        // "Bạch Mai" là tên đường trong đoạn đầu: giữ nguyên
        assertThat(
            plan("SỐ 55 NGÕ 295 BẠCH MAI, Q. HAI BÀ TRƯNG", "Phường Bạch Mai", "Thành phố Hà Nội", HN_DISTRICTS).addressNew()
        ).isEqualTo("SỐ 55 NGÕ 295 BẠCH MAI, Phường Bạch Mai, Thành phố Hà Nội");
    }

    @Test
    void thanhPhoThuocTinhVaTinhGop() {
        // "TP. BẮC NINH" là thành phố thuộc tỉnh (cấp huyện), "BẮC NINH" là tỉnh
        assertThat(
            plan(
                "11A LÝ THÁI TỔ, P. SUỐI HOA, TP. BẮC NINH, BẮC NINH",
                "Phường Kinh Bắc",
                "Tỉnh Bắc Ninh",
                Set.of("bac ninh", "tu son")
            ).addressNew()
        ).isEqualTo("11A LÝ THÁI TỔ, Phường Kinh Bắc, Tỉnh Bắc Ninh");
        assertThat(
            plan(
                "Số 164 đường Xuân Diệu, Phường Trần Phú, Thành phố Quy Nhơn, Tỉnh Bình Định, Việt Nam",
                "Phường Quy Nhơn",
                "Tỉnh Gia Lai",
                Set.of("quy nhon")
            ).addressNew()
        ).isEqualTo("Số 164 đường Xuân Diệu, Phường Quy Nhơn, Tỉnh Gia Lai");
        // Giữ "Khu 1", "KP 1"
        assertThat(
            plan(
                "Số 27, Khu 1, Thị Trấn Tân Phú, Huyện Tân Phú, Tỉnh Đồng Nai, Việt Nam",
                "Xã Tân Phú",
                "Tỉnh Đồng Nai",
                Set.of("tan phu")
            ).addressNew()
        ).isEqualTo("Số 27, Khu 1, Xã Tân Phú, Tỉnh Đồng Nai");
    }

    @Test
    void daLaDiaChiMoiThiKhongTao() {
        Result r = plan("Số 47, phố Tôn Thất Thiệp, Phường Hoàn Kiếm, TP Hà Nội", "Phường Hoàn Kiếm", "Thành phố Hà Nội", HN_DISTRICTS);
        assertThat(r.addressNew()).isNull();
        assertThat(r.outcome()).isEqualTo(NewAddressPlanner.ALREADY_NEW);
        // Cùng tên xã nhưng còn quận: vẫn là địa chỉ cũ
        assertThat(
            plan("888 La Thành, P. Giảng Võ, Q. Ba Đình, TP. Hà Nội", "Phường Giảng Võ", "Thành phố Hà Nội", HN_DISTRICTS).outcome()
        ).isEqualTo(NewAddressPlanner.CREATED);
        // Thành phố Thủ Đức (đã bỏ) là cấp huyện
        assertThat(
            plan(
                "177 Tam Bình, Phường Tam Phú, Thành phố Thủ Đức, Thành phố Hồ Chí Minh, Việt Nam",
                "Phường Tam Bình",
                "Thành phố Hồ Chí Minh",
                Set.of("thu duc")
            ).addressNew()
        ).isEqualTo("177 Tam Bình, Phường Tam Bình, Thành phố Hồ Chí Minh");
    }

    @Test
    void catDonViHanhChinhVietDinhThieuDauPhay() {
        Set<String> hcm = Set.of("binh thanh", "7");
        assertThat(
            plan(
                "P112 KHU A9 CC BẮC ĐINH BỘ LĨNH P.26, Q.BÌNH THẠNH, TP.HỒ CHÍ MINH",
                "Phường Bình Thạnh",
                "Thành phố Hồ Chí Minh",
                hcm
            ).addressNew()
        ).isEqualTo("P112 KHU A9 CC BẮC ĐINH BỘ LĨNH, Phường Bình Thạnh, Thành phố Hồ Chí Minh");
        assertThat(
            plan("101A KP MỸ HƯNG P.TÂN PHONG, Q.7, TP.HỒ CHÍ MINH", "Phường Tân Hưng", "Thành phố Hồ Chí Minh", hcm).addressNew()
        ).isEqualTo("101A KP MỸ HƯNG, Phường Tân Hưng, Thành phố Hồ Chí Minh");
        // Không cắt "P112", "Lô P"
        assertThat(plan("Lô P, P112 Tòa A", "Phường X", "Tỉnh Y", Set.of()).addressNew()).isEqualTo("Lô P, P112 Tòa A, Phường X, Tỉnh Y");
    }

    @Test
    void diaChiDungGachNgangLamDauPhanCach() {
        assertThat(
            plan(
                "82 Phước Lăng - Xã Tam Phước - Huyện Long Điền - Bà Rịa - Vũng Tàu.",
                "Xã Long Điền",
                "Thành phố Hồ Chí Minh",
                Set.of("long dien", "ba ria", "vung tau")
            ).addressNew()
        ).isEqualTo("82 Phước Lăng, Xã Long Điền, Thành phố Hồ Chí Minh");
        assertThat(
            plan("48 Nguyễn Phi Khanh - - Quận 1 - TP Hồ Chí Minh.", "Phường Tân Định", "Thành phố Hồ Chí Minh", Set.of("1")).addressNew()
        ).isEqualTo("48 Nguyễn Phi Khanh, Phường Tân Định, Thành phố Hồ Chí Minh");
        // Có dấu phẩy: " - " trong tên đường giữ nguyên
        assertThat(
            plan(
                "Lô CN09, đường cao tốc cầu Giẽ - Ninh Bình, Phường Tiên Nội, Thị Xã Duy Tiên, Tỉnh Hà Nam",
                "Phường Duy Tiên",
                "Tỉnh Ninh Bình",
                Set.of("duy tien")
            ).addressNew()
        ).isEqualTo("Lô CN09, đường cao tốc cầu Giẽ - Ninh Bình, Phường Duy Tiên, Tỉnh Ninh Bình");
    }

    @Test
    void khongBoNhamDuongGiaoThong() {
        assertThat(plan("Số 9, Xa lộ Hà Nội", "Phường Thủ Đức", "Thành phố Hồ Chí Minh", Set.of()).addressNew()).isEqualTo(
            "Số 9, Xa lộ Hà Nội, Phường Thủ Đức, Thành phố Hồ Chí Minh"
        );
        assertThat(plan("Km 5, Tỉnh lộ 10, TP.HCM", "Phường Bình Tân", "Thành phố Hồ Chí Minh", Set.of()).addressNew()).isEqualTo(
            "Km 5, Tỉnh lộ 10, Phường Bình Tân, Thành phố Hồ Chí Minh"
        );
    }

    @Test
    void khongDuThongTinThiKhongTao() {
        assertThat(plan(null, "Phường A", "Tỉnh B", Set.of()).outcome()).isEqualTo(NewAddressPlanner.EMPTY);
        assertThat(plan("  ", "Phường A", "Tỉnh B", Set.of()).addressNew()).isNull();
        assertThat(plan("Số 1, Nam Từ Liêm, Hà Nội", null, "Thành phố Hà Nội", HN_DISTRICTS).outcome()).isEqualTo(
            NewAddressPlanner.NO_NEW_WARD
        );
        // Chỉ có đoạn hành chính: địa chỉ mới chỉ gồm xã, tỉnh
        assertThat(plan("Phường Phúc Xá, Quận Ba Đình, Hà Nội", "Phường Hồng Hà", "Thành phố Hà Nội", HN_DISTRICTS).addressNew()).isEqualTo(
            "Phường Hồng Hà, Thành phố Hà Nội"
        );
    }
}
