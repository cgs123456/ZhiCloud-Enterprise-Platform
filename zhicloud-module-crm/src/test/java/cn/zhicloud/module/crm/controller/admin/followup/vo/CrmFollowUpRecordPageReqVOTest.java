package cn.zhicloud.module.crm.controller.admin.followup.vo;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrmFollowUpRecordPageReqVOTest {

    private static final Validator VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    @Test
    void pageRequestRequiresBizScope() {
        CrmFollowUpRecordPageReqVO request = new CrmFollowUpRecordPageReqVO();

        assertThat(VALIDATOR.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("bizType", "bizId");
    }
}
