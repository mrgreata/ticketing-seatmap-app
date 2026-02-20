import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

/* ===== Core Components ===== */
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './auth/login/login.component';
import { MessageComponent } from './components/message/message.component';
import { RegistrationComponent } from './auth/registration/registration.component';

/* ===== Password Reset ===== */
import { RequestComponent } from './auth/password-reset/request/request.component';
import { ConfirmComponent } from './auth/password-reset/confirm/confirm.component';


/* ===== Events ===== */
import { EventListComponent } from './components/event/event-list.component';
import { EventDetailComponent } from './components/event/event-detail.component';

/* ===== Admin ===== */
import { UserManagementComponent } from './admin/user-management/user-management.component';
import { CreateUserComponent } from './admin/create-user/create-user.component';

/* ===== Shared ===== */
import { SharedModule } from './shared/shared.module';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ToastrModule } from 'ngx-toastr';
import { httpInterceptorProviders } from './interceptors';
import {NewsDetailComponent} from "./components/news/newsDetail/news-detail.component";
import {NewsHomeComponent} from "./components/news/newsHome/news-home.component";

@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    FooterComponent,
    HomeComponent,
    LoginComponent,
    MessageComponent,
    RegistrationComponent,
    RequestComponent,
    ConfirmComponent,

    EventListComponent,
    EventDetailComponent,

    NewsDetailComponent,
    NewsHomeComponent,

    UserManagementComponent,
    CreateUserComponent,

  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    AppRoutingModule,
    ReactiveFormsModule,
    FormsModule,
    NgbModule,
    SharedModule,
    ToastrModule.forRoot({
      positionClass: 'toast-top-right',
      timeOut: 5000,
      closeButton: true,
      progressBar: true,
      preventDuplicates: true,
      newestOnTop: true
    })
  ],
  providers: [
    httpInterceptorProviders,
    provideHttpClient(withInterceptorsFromDi())
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
