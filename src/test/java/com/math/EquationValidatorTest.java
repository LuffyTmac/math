package com.math;

import com.math.util.EquationValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自定义等式校验测试
 */
public class EquationValidatorTest {

    @Test
    void correctEquation() {
        EquationValidator.Result r = EquationValidator.validate("3×(4+5)=3×4+3×5");
        assertTrue(r.isCorrect());
        assertEquals("3×(4+5)=3×4+3×5", r.getDisplay());
    }

    @Test
    void correctWithSpacesAndAsterisk() {
        EquationValidator.Result r = EquationValidator.validate(" 3 * ( 4 + 5 ) = 3 * 4 + 3 * 5 ");
        assertTrue(r.isCorrect());
        assertEquals("3×(4+5)=3×4+3×5", r.getDisplay());
    }

    @Test
    void correctWithSwappedProducts() {
        assertTrue(EquationValidator.validate("3×(4+5)=3×5+3×4").isCorrect());
    }

    @Test
    void correctMinus() {
        assertTrue(EquationValidator.validate("3×(9-2)=3×9-3×2").isCorrect());
    }

    @Test
    void wrongNumbers() {
        EquationValidator.Result r = EquationValidator.validate("3×(4+5)=3×4+4×5");
        assertFalse(r.isCorrect());
        assertEquals("3×(4+5)=3×4+4×5", r.getDisplay());
    }

    @Test
    void wrongFactor() {
        assertFalse(EquationValidator.validate("2×(4+5)=3×4+3×5").isCorrect());
    }

    @Test
    void wrongOperator() {
        assertFalse(EquationValidator.validate("3×(4+5)=3×4-3×5").isCorrect());
    }

    @Test
    void invalidFormat() {
        EquationValidator.Result r = EquationValidator.validate("3×(4+5+6)=3×4+3×5+3×6");
        assertFalse(r.isCorrect());
        assertTrue(r.getMessage().contains("格式"));
    }

    @Test
    void missingEquals() {
        assertFalse(EquationValidator.validate("3×(4+5)").isCorrect());
    }

    @Test
    void empty() {
        EquationValidator.Result r = EquationValidator.validate("   ");
        assertEquals("", r.getDisplay());
        assertFalse(r.isCorrect());
    }
}
