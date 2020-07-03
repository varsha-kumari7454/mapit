import { Component, OnInit } from '@angular/core';
import { DataService } from '../data.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router,NavigationExtras } from '@angular/router';
import { ActivatedRoute }       from '@angular/router';
import { map }                  from 'rxjs/operators';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

  loginForm: FormGroup;
  registrationForm: FormGroup;
  users: Object;
  submitted: boolean = false;
  success: boolean = false;

  constructor(private data: DataService, private formBuilder: FormBuilder, private router: Router, private route: ActivatedRoute, private toastr: ToastrService) { }

  ngOnInit() {
    this.loginForm = this.formBuilder.group({
      email: ['', Validators.required],
      password: ['', Validators.required]
    });
    this.registrationForm = this.formBuilder.group({
      email: ['', Validators.required],
      name: ['', Validators.required],
      password: ['', Validators.required],
      confirmPassword: ['', Validators.required]
    });
    

    //this.data.getUsers().subscribe(data => {
      //this.users = data;
      //console.log(this.users);
    //});
  }

  register() {
    console.log(this.registrationForm.value.email);
    let publicAppId = this.route.snapshot.queryParamMap.get('publicAppId');
    let role = this.route.snapshot.queryParamMap.get('role');
    let redirectTo = this.route.snapshot.queryParamMap.get('redirectTo');

    let user:any= {};
    user.name = this.registrationForm.value.name;
    user.email = this.registrationForm.value.email;
    user.password = this.registrationForm.value.password;
    user.role = role;

        
    this.data.createUser(user, publicAppId, redirectTo).subscribe(data => {
        console.log(data);
        this.toastr.success('Hello world!', 'Toastr fun!');
      },
      error => {
        this.toastr.error('Hello world!', 'Toastr fun!');
      });

  }

  redirect() {
    let navigationExtras: NavigationExtras = {
        queryParams: {
            "publicAppId": "publicAppId",
            "role": "role",
            "redirectTo": "redirectTo"
        }
    };
    this.router.navigate(["/login"], navigationExtras);
  }
  login() {
    console.log(this.loginForm.value.email);
  this.data.log("Login Clicked");
    this.submitted = true;

    if (this.loginForm.invalid) {
        return;
    }
    this.success = true;
  }
}
