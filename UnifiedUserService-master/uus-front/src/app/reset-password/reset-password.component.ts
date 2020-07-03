import { Component, OnInit } from "@angular/core";
import { DataService } from "../data.service";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { Router, NavigationExtras } from "@angular/router";
import { ActivatedRoute } from "@angular/router";
import { map } from "rxjs/operators";
import { ToastrService } from "ngx-toastr";
import { stringify } from '@angular/compiler/src/util';
import { ConstantsService } from '../../app/common/services/constants.service';

@Component({
  selector: "app-reset-password",
  templateUrl: "./reset-password.component.html",
  styleUrls: ["./reset-password.component.scss"]
})
export class ResetPasswordComponent implements OnInit {
  resetPasswordForm: FormGroup;
  submitted: boolean = false;
  success: boolean = false;

  constructor(
    private dataService: DataService,
    private formBuilder: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private toastr: ToastrService,
    private _constant: ConstantsService
  ) {}

  ngOnInit() {
    this.resetPasswordForm = this.formBuilder.group({
      password: ["", Validators.required],
      confirmPassword: ["", Validators.required]
    });
  }

  hasError = false;
  doAsyncTask(){
    let promise = new Promise((resolve,reject) => {
      setTimeout(() =>{        
        if(this.hasError){
          resolve();
        }else{
          reject();
        }
      },5000);
    });
    this.hasError = true;
    return promise;
  }
  resetPassword() {
    this.dataService.log("resetPassword called");
    
    let publicAppId = this.route.snapshot.queryParamMap.get("publicAppId");
    let email = this.route.snapshot.queryParamMap.get("email");
    let redirectLink = this.route.snapshot.queryParamMap.get("redirectLink");
    let resetPasswordCodeHash = this.route.snapshot.queryParamMap.get(
      "resetPasswordCodeHash"
    );

    let user: any = {};
    user.email = email;
    user.password = this.resetPasswordForm.value.password;

    this.submitted = true;
    if (this.resetPasswordForm.invalid) {
      return;
    }

    this.dataService
      .resetPassword(user, publicAppId, redirectLink, resetPasswordCodeHash)
      .subscribe(
        data => {
          this.toastr.success("success!", "Your password is reset!");
          this.success = true;                    
          console.log(data);                  
          this.doAsyncTask().then((key) => {            
            window.location.href = stringify(data.body)
          })
        },
        error => {
          
          if(error.status == 722){
            this.toastr.warning(
              "Invalid Link",
            )
            return;
          }
          
          this.toastr.error(
            "Error!",
            "Your password is not reset !" + error.data
          );
          
        }
      );
  }
}
