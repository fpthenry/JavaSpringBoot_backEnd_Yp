package com.mycompany.myapp.service.listingimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ImportFileReaderTest {

    @TempDir
    Path dir;

    private List<RecordSource.SourceRow> readAll(RecordSource s) {
        List<RecordSource.SourceRow> rows = new ArrayList<>();
        try (s) {
            s.forEachRemaining(rows::add);
        }
        return rows;
    }

    @Test
    void csvWithVietnameseHeadersSemicolonAndBom() throws Exception {
        Path f = dir.resolve("dn.csv");
        String content =
            "﻿Tên doanh nghiệp;Mã số thuế;Số điện thoại;Địa chỉ;Cột lạ\n" +
            "Công ty TNHH Ánh Dương;101234567;0912 345 678;\"Số 1, Hà Nội\";x\n" +
            "\n" +
            "Công ty B;;;;\n";
        Files.writeString(f, content, StandardCharsets.UTF_8);

        List<RecordSource.SourceRow> rows = readAll(ImportFileReader.open(f, "dn.csv"));

        assertThat(rows).hasSize(2);
        ImportRecord r = rows.get(0).record();
        assertThat(rows.get(0).rowNo()).isEqualTo(2);
        assertThat(r.name()).isEqualTo("Công ty TNHH Ánh Dương");
        assertThat(r.taxCode()).isEqualTo("101234567");
        assertThat(r.phone()).isEqualTo("0912 345 678");
        assertThat(r.address()).isEqualTo("Số 1, Hà Nội");
        assertThat(rows.get(1).record().name()).isEqualTo("Công ty B");
        assertThat(rows.get(1).record().taxCode()).isNull();
    }

    @Test
    void csvStandardHeadersComma() throws Exception {
        Path f = dir.resolve("std.csv");
        Files.writeString(f, "name,taxCode,categories,province\nABC,0101234567,\"4690; 4610\",Hà Nội\n", StandardCharsets.UTF_8);

        ImportRecord r = readAll(ImportFileReader.open(f, "std.csv")).get(0).record();

        assertThat(r.categories()).isEqualTo("4690; 4610");
        assertThat(r.province()).isEqualTo("Hà Nội");
    }

    @Test
    void csvWithoutNameColumnIsRejected() throws Exception {
        Path f = dir.resolve("bad.csv");
        Files.writeString(f, "mst,phone\n0101234567,0912345678\n", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> ImportFileReader.open(f, "bad.csv"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tên doanh nghiệp");
    }

    @Test
    void unsupportedExtensionIsRejected() {
        assertThatThrownBy(() -> ImportFileReader.open(dir.resolve("a.xls"), "a.xls")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ImportFileReader.open(dir.resolve("a.pdf"), "a.pdf")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void apiJsonValues() {
        ObjectMapper om = JsonMapper.builder().build();
        var node = om.readTree(
            "{\"data\":{\"items\":[{\"ten_cong_ty\":\"ABC\",\"mst\":\"0101234567\",\"nganh\":[{\"code\":\"4690\"},{\"code\":\"4610\"}]}]}}"
        );
        var items = ApiRecordSource.path(node, "data.items");
        assertThat(items.isArray()).isTrue();
        assertThat(ApiRecordSource.text(items.path(0).path("nganh"))).isEqualTo("4690; 4610");
        assertThat(ApiRecordSource.text(ApiRecordSource.path(node, "data.items.0.mst"))).isEqualTo("0101234567");
        assertThat(ImportRecord.fieldOf("ten_cong_ty")).isEqualTo("name");
        assertThat(ImportRecord.fieldOf("Mã số thuế")).isEqualTo("taxCode");
    }
}
