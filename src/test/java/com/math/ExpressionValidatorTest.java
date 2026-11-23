package com.math;

import com.math.util.ExpressionValidator;
import com.math.util.ExpressionValidator.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 乘法分配律算式校验单元测试
 */
class ExpressionValidatorTest {

    @Test
    void acceptsDistributeForm() {
        assertOk("3*(4+5)", "3*(4+5)");
        assertOk("12*(30-7)", "12*(30-7)");
        assertOk("6*(10+2)", "6*(10+2)");
    }

    @Test
    void acceptsExpandForm() {
        assertOk("3*4+3*5", "3*4+3*5");
        assertOk("5*20-5*3", "5*20-5*3");
        // 相同因数在后：4*3 + 5*3
        assertOk("4*3+5*3", "4*3+5*3");
    }

    @Test
    void normalizesFullWidthAndSpaces() {
        assertOk("3×（4＋5）", "3*(4+5)");
        assertOk(" 3 * 4 + 3 * 5 ", "3*4+3*5");
        assertOk("３×（４＋５）", "3*(4+5)");
        assertOk("3x(4+5)", "3*(4+5)");
        assertEquals("3*(4+5)", ExpressionValidator.normalize("３ × （４ ＋ ５）"));
    }

    @Test
    void rejectsEmpty() {
        Result r = ExpressionValidator.validate("   ");
        assertFalse(r.isValid());
        assertEquals("先写一个算式吧～", r.getMessage());
    }

    @Test
    void rejectsIllegalCharacters() {
        Result r = ExpressionValidator.validate("abc");
        assertFalse(r.isValid());
        assertEquals("只能用数字和 + - × ( ) 哦～", r.getMessage());

        Result r2 = ExpressionValidator.validate("3*4=12");
        assertFalse(r2.isValid());
        assertEquals("只能用数字和 + - × ( ) 哦～", r2.getMessage());
    }

    @Test
    void rejectsWithoutMultiply() {
        Result r = ExpressionValidator.validate("1+1");
        assertFalse(r.isValid());
        assertEquals("分配律里要出现乘号 × 哦～", r.getMessage());
    }

    @Test
    void rejectsUnbalancedBrackets() {
        Result r = ExpressionValidator.validate("3*(4+5");
        assertFalse(r.isValid());
        assertEquals("括号要成对出现，像这样：(4+5)", r.getMessage());

        Result r2 = ExpressionValidator.validate("3*4+5)");
        assertFalse(r2.isValid());
        assertEquals("括号要成对出现，像这样：(4+5)", r2.getMessage());
    }

    @Test
    void rejectsExpandWithoutCommonFactor() {
        Result r = ExpressionValidator.validate("3*4+5*6");
        assertFalse(r.isValid());
        assertEquals("两个乘法要有一个相同的数，比如 3×4＋3×5 里的 3 哦～", r.getMessage());
    }

    @Test
    void rejectsWrongBracketContent() {
        Result r = ExpressionValidator.validate("3*(4+5+6)");
        assertFalse(r.isValid());
        assertEquals("括号里写成「4+5」这样就好啦～", r.getMessage());

        Result r2 = ExpressionValidator.validate("3*()");
        assertFalse(r2.isValid());
        assertEquals("括号里写成「4+5」这样就好啦～", r2.getMessage());
    }

    @Test
    void rejectsOtherShapes() {
        Result r = ExpressionValidator.validate("3*4*5");
        assertFalse(r.isValid());
        assertEquals("试试写成 3×(4+5) 或 3×4＋3×5 吧～", r.getMessage());
    }

    private void assertOk(String raw, String expectedNormalized) {
        Result r = ExpressionValidator.validate(raw);
        assertTrue(r.isValid(), "应通过：" + raw + "，实际提示：" + r.getMessage());
        assertEquals(expectedNormalized, r.getNormalized());
    }
}
