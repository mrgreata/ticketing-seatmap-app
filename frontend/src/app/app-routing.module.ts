import { NgModule } from '@angular/core';
import { RouterModule, Routes, mapToCanActivate } from '@angular/router';

import { AuthGuard } from './guards/auth.guard';

/* ===== Public & Core ===== */
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './auth/login/login.component';
import { RegistrationComponent } from './auth/registration/registration.component';
import { MessageComponent } from './components/message/message.component';
import { ProfileComponent } from './components/profile/profile.component';
import { RequestComponent } from './auth/password-reset/request/request.component';
import { ConfirmComponent } from './auth/password-reset/confirm/confirm.component';

/* ===== News ===== */
import { NewsHomeComponent } from './components/news/newsHome/news-home.component';
import { NewsDetailComponent } from './components/news/newsDetail/news-detail.component';
import { NewsCreateComponent } from './components/news/newsCreate/news-create.component';

/* ===== Events ===== */
import { EventListComponent } from './components/event/event-list.component';
import { EventDetailComponent } from './components/event/event-detail.component';
import { EventCreateComponent } from './components/event/event-create.component';
import { EventEditComponent } from './components/event/event-edit.component';

/* ===== Tickets ===== */
import { TicketOverviewComponent } from './components/ticket/ticketOverview/ticket-overview.component';
import { TicketInvoicesComponent } from './components/profile/ticket-invoices/ticket-invoices.component';

/* ===== Seatmap ===== */
import { SeatmapComponent } from './components/seatmap/seatmap.component';

/* ===== Merchandise ===== */
import { MerchandiseListComponent } from './components/merchandise/merchandiseList/merchandise-list.component';
import { MerchandiseDetailComponent } from './components/merchandise/merchandiseDetail/merchandise-detail.component';
import { MerchandiseRewardsComponent } from './components/merchandise/merchandiseRewards/merchandise-rewards.component';
import { CheckoutComponent} from "./components/checkout/checkout.component";
import { MerchandiseCreateComponent } from "./components/merchandise/mechandiseCreate/merchandise-create.component";
import { MerchandiseInvoicesComponent } from './components/profile/merchandise-invoices/merchandise-invoices.component';


/* ===== Cart ===== */
import { CartComponent } from './components/cart/cart.component'

/* ===== Admin ===== */
import { UserManagementComponent } from './admin/user-management/user-management.component';
import { CreateUserComponent } from './admin/create-user/create-user.component';
import {ProfileEditComponent} from "./components/profile/profileEdit/profile-edit.component";
import {NewsEditComponent} from "./components/news/newsEdit/news-edit.component";

const routes: Routes = [

  /* ===== Public ===== */
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'registration', component: RegistrationComponent },

  {
    path: 'password-reset',
    children: [
      { path: 'request', component: RequestComponent },
      { path: 'confirm', component: ConfirmComponent }
    ]
  },

  /* ===== Authenticated General ===== */
  {
    path: 'message',
    component: MessageComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },
  {
    path: 'profile',
    component: ProfileComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },
  {
    path: 'profile/edit',
    component: ProfileEditComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },

  /* ===== News ===== */
  {
    path: 'news/home',
    component: NewsHomeComponent
  },
  {
    path: 'news/detail/:id',
    component: NewsDetailComponent
  },
  {
    path: 'admin/news/creation',
    component: NewsCreateComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },
  {
    path: 'admin/news/edit/:id',
    component: NewsEditComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },

  /* ===== Events ===== */
  { path: 'events', component: EventListComponent },
  {
    path: 'events/new',
    component: EventCreateComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },
  {
    path: 'events/:id/edit',
    component: EventEditComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },
  { path: 'events/:id', component: EventDetailComponent },

  /* ===== Tickets ===== */
  {
    path: 'tickets/my',
    component: TicketOverviewComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },
  {
    path: 'profile/ticket-invoices',
    component: TicketInvoicesComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },

  /* ===== Seatmap ===== */
  {
    path: 'seatmap',
    component: SeatmapComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },

  /* ===== Merchandise ===== */
  {
    path: 'merchandise',
    component: MerchandiseListComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },
  {
    path: 'merchandise/rewards',
    component: MerchandiseRewardsComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },
  {
    path: 'merchandise/checkout',
    component: CheckoutComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },
  {
    path: 'merchandise/:id',
    component: MerchandiseDetailComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },
  {
    path: 'profile/merchandise-invoices',
    component: MerchandiseInvoicesComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },

  /* ===== Cart ===== */
  {
    path: 'cart',
    component: CartComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },
  {
    path: 'checkout',
    component: CheckoutComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },


  /* ===== Admin ===== */
  {
    path: 'admin/user-management',
    component: UserManagementComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },
  {
    path: 'admin/users/creation',
    component: CreateUserComponent,
    canActivate: mapToCanActivate([AuthGuard])
  },
  {
    path: 'admin/merchandise/creation',
    component: MerchandiseCreateComponent,
    canActivate: mapToCanActivate([AuthGuard])
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { useHash: true })],
  exports: [RouterModule]
})
export class AppRoutingModule {}
