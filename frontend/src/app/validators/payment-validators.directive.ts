import { Directive, forwardRef } from '@angular/core';
import { AbstractControl, NG_VALIDATORS, ValidationErrors, Validator } from '@angular/forms';

@Directive({
  selector: '[luhnValidator]',
  providers: [
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => LuhnValidatorDirective), multi: true }
  ],
  standalone: true
})
export class LuhnValidatorDirective implements Validator {
  validate(control: AbstractControl): ValidationErrors | null {
    const digits = String(control.value ?? '').trim();
    if (!digits) return null;

    if (!/^\d{16}$/.test(digits)) return null;

    return this.passesLuhnCheck(digits) ? null : { luhn: true };
  }

  private passesLuhnCheck(digits: string): boolean {
    let sum = 0;
    let isSecond = false;

    for (let i = digits.length - 1; i >= 0; i--) {
      let d = digits.charCodeAt(i) - 48;
      if (isSecond) {
        d = d * 2;
        sum += Math.floor(d / 10);
        sum += d % 10;
      } else {
        sum += d;
      }
      isSecond = !isSecond;
    }
    return sum % 10 === 0;
  }
}

@Directive({
  selector: '[notExpiredValidator]',
  providers: [
    { provide: NG_VALIDATORS, useExisting: forwardRef(() => NotExpiredValidatorDirective), multi: true }
  ],
  standalone: true
})
export class NotExpiredValidatorDirective implements Validator {
  validate(control: AbstractControl): ValidationErrors | null {
    const mmyy = String(control.value ?? '').trim();
    if (!mmyy) return null;

    if (!/^(0[1-9]|1[0-2])\d{2}$/.test(mmyy)) return null;

    const month = Number(mmyy.substring(0, 2));
    const year = 2000 + Number(mmyy.substring(2, 4));

    const now = new Date();
    const nowYear = now.getFullYear();
    const nowMonth = now.getMonth() + 1;

    const expired = year < nowYear || (year === nowYear && month < nowMonth);
    return expired ? { expired: true } : null;
  }
}
