import { Component, OnInit } from '@angular/core';
import { DataService } from "../data.service";
import { ToastrService } from "ngx-toastr";
import { Router,NavigationExtras } from '@angular/router';

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent implements OnInit {

  constructor(
    private dataService: DataService,
    private toastr: ToastrService,
    private router: Router,
  ) { }

  ngOnInit() {
  }
  password : string;
  email : string;
  login(){

    console.log(this.email);
    this.dataService
    
      .login(this.email , this.password)
      .subscribe(
        data => {
          console.log(data);
          this.toastr.success(data.toString());
          console.log('redirecting');
          this.router.navigate(["/email/update"]);
        },
      error => {
        console.log(error.error)
        this.toastr.error(error.error);
      });

   }
}
