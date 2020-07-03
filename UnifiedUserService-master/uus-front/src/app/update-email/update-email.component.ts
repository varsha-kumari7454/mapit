import { Component, OnInit } from '@angular/core';
import { DataService } from "../data.service";
import { ActivatedRoute } from "@angular/router";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { Router, NavigationExtras } from "@angular/router";
import { map } from "rxjs/operators";
import { ToastrService } from "ngx-toastr";
import { stringify } from '@angular/compiler/src/util';
import { ConstantsService } from '../../app/common/services/constants.service';
import updateEmailHistory from './updateEmailHistory';
import updateEmailHistories from './updateEmailHistories';
import { ListKeyManager } from '@angular/cdk/a11y';
@Component({
  selector: 'app-update-email',
  templateUrl: './update-email.component.html',
  styleUrls: ['./update-email.component.scss']
})
 //var EmailHistory;
export class UpdateEmailComponent implements OnInit {

  constructor(private dataService: DataService,
    private formBuilder: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private toastr: ToastrService,
    private _constant: ConstantsService) { }

     
  ngOnInit() {
    this.emailUpdateHistory();
    this.isValid = true;
    this.isNewEmailValid = true;
    
  }
  EmailHistory : object;
  email : string;
  newEmail : string;
  comment : string;
  isValid : boolean;
  isNewEmailValid : boolean;

  validation(){
    var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    this.isValid = re.test(String(this.email).toLowerCase()) || this.email == "";
  }

  validationNew(){
    var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    this.isNewEmailValid = re.test(String(this.newEmail).toLowerCase()) || this.newEmail == "";
  }

  updateEmail(){
    console.log("update email");

    console.log(this.email);
    this.dataService
    
      .updateEmail(this.email , this.newEmail,this.comment)
      .subscribe(
        data => {
          console.log(data);
          this.toastr.success(data.toString());
          this.emailUpdateHistory();
        },
      error => {
        console.log(error.error)
        this.toastr.error(error.error);
        this.emailUpdateHistory();
      });
    }

    logout(){
      console.log("logout called");
      this.dataService
      
        .logout()
        .subscribe(
          data => {
            console.log(data);
            this.toastr.success(data.body.toString());
            this.router.navigate(["/admin"]);
          },
        error => {
          console.log(error.error)
          this.toastr.error(error.error);
          this.router.navigate(["/admin"]);
        });
     }

   emailUpdateHistory(){
    console.log("emailUpdateHistory called");
    this.dataService
    
      .emailUpdateHistory()
      .subscribe(
        data => {
         this.EmailHistory = data.body;
         this.updateEmailHistories = this.EmailHistory;
          
        },
      error => {
        console.log(error.error)
        this.toastr.error(error.error);
        this.router.navigate(["/admin"]);
      });

   }
   
   updateEmailHistories = this.EmailHistory;
  
}
