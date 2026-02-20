import { Injectable, SecurityContext } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { HttpErrorResponse } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ErrorFormatterService {

  constructor(private sanitizer: DomSanitizer) {}

  format(error: HttpErrorResponse): string {
    if (!error) {
      return 'Ein unerwarteter Fehler ist aufgetreten.';
    }


    if (Array.isArray(error.error?.errors) && error.error.errors.length > 0) {
      return this.formatBackendErrors(error.error.errors);
    }

    if (typeof error.error?.message === 'string') {
      const translated = this.translateError(error.error.message);

      if (translated !== error.error.message) {
        return translated;
      }
    }

    return this.formatByStatus(error.status);
  }

  private formatBackendErrors(errors: string[]): string {
    const translatedErrors = errors.map(e => this.translateError(e));

    if (translatedErrors.length === 1) {
      return translatedErrors[0];
    }

    let message = 'Es sind mehrere Fehler aufgetreten:<ul>';

    for (const err of translatedErrors) {
      const sanitized = this.sanitizer.sanitize(SecurityContext.HTML, err);
      message += `<li>${sanitized}</li>`;
    }

    message += '</ul>';
    return message;
  }

  private translateError(error: string): string {
    const normalized = error.toLowerCase();

    if (
      normalized.includes('reset token') &&
      (normalized.includes('invalid') || normalized.includes('expired'))
    ) {
      return 'Der Link zum Zurücksetzen des Passworts ist ungültig oder abgelaufen.';
    }


    if (normalized.includes('email already in use')) {
      return 'Diese E-Mail-Adresse kann nicht vergeben werden.';
    }

    if (normalized.includes('user role must be')) {
      return 'Die ausgewählte Benutzerrolle ist ungültig.';
    }

    if (normalized.includes('user not found')) {
      return 'E-Mail-Adresse oder Passwort ist falsch.';
    }

    if (normalized.includes('invalid credentials')) {
      return 'E-Mail-Adresse oder Passwort ist falsch.';
    }

    if (normalized.includes('must not be null')) {
      return 'Ein Pflichtfeld wurde nicht ausgefüllt.';
    }

    if (normalized.includes('validation')) {
      return 'Die eingegebenen Daten sind ungültig.';
    }
    if (normalized.includes('administrators cannot lock or unlock')) {
      return 'Sie können sich nicht selbst sperren.';
    }

    if (normalized.includes('cart is empty')) {
      return 'Der Warenkorb ist leer.';
    }

    if (normalized.includes('insufficient stock') || normalized.includes('not enough stock')) {
      return 'Nicht genügend Bestand verfügbar.';
    }

    if (normalized.includes('reservation expired')) {
      return 'Die Reservierung ist abgelaufen.';
    }

    if (normalized.includes('ticket') && normalized.includes('not available')) {
      return 'Dieses Ticket ist nicht mehr verfügbar.';
    }

    if (normalized.includes('payment') && normalized.includes('invalid')) {
      return 'Zahlungsdaten sind ungültig.';
    }

    if (normalized.includes('insufficient reward points')) {
      return 'Nicht genügend Prämienpunkte vorhanden.';
    }

    return this.sanitizeFallback(error);
  }

  private sanitizeFallback(error: string): string {
    return (
      this.sanitizer.sanitize(SecurityContext.HTML, error) ??
      'Ein ungültiger Wert wurde übermittelt.'
    );
  }

  private formatByStatus(status: number): string {
    switch (status) {
      case 400:
        return 'Ungültige Anfrage. Bitte überprüfen Sie Ihre Eingaben.';
      case 401:
        return 'E-Mail oder Passwort sind falsch.';
      case 403:
        return 'Sie haben keine Berechtigung für diese Aktion.';
      case 404:
        return 'E-Mail oder Passwort sind falsch.';
      case 409:
        return 'Ein Konflikt ist aufgetreten.';
      case 422:
        return 'Die eingegebenen Daten sind ungültig.';
      case 423:
        return 'Ihr Konto ist gesperrt.';
      default:
        return 'Ein unerwarteter Fehler ist aufgetreten.';
    }
  }
}
