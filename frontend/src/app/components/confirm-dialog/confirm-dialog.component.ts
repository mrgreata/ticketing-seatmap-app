import { Component, EventEmitter, HostBinding, Input, Output } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  standalone: false,
  templateUrl: './confirm-dialog.component.html',
  styleUrls: ['./confirm-dialog.component.scss']
})
export class ConfirmDialogComponent {

  @Input() title = 'Confirm action';
  @Input() message = 'Are you sure?';
  @Input() confirmText = 'Confirm';
  @Input() cancelText = 'Cancel';
  @Input() confirmButtonClass = 'btn-danger';

  @Output() confirm = new EventEmitter<void>();

  @HostBinding('class') cssClass = 'modal fade';
  @HostBinding('attr.data-bs-backdrop') backdrop = 'static';
  @HostBinding('attr.aria-hidden') hidden = 'true';
  @HostBinding('attr.aria-labelledby') labelledBy = 'confirm-dialog-title';
}
