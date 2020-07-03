import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ConstantsService } from '../app/common/services/constants.service';
import { CompilerConfig } from '@angular/compiler';

@Injectable({
  providedIn: 'root'
})
export class DataService {

  constructor(private http: HttpClient, private _constant: ConstantsService) { }

  log(message) {
    return console.log(message);
  }

  createUser(user, publicAppId, redirectTo) {
    return this.http.post(this._constant.uusUrl + '/ajax/user/register' 
        + '?publicAppId=' + publicAppId 
        + '&redirectLink=' + redirectTo, user)
  }

  resetPassword(user, publicAppId, redirectTo, resetPasswordCodeHash) {
    return this.http.post<CompilerConfig>(this._constant.uusUrl + '/ajax/user/password/reset'
      + '?publicAppId=' + publicAppId 
      + '&redirectLink=' + redirectTo 
      + '&validationHash=' + resetPasswordCodeHash, user,{
        responseType:'json',
        observe: 'response' })
  }

  updateEmail(email , newEmail, comment) {
    console.log(email);
    return this.http.post(this._constant.uusUrl + '/ajax/user/email/reset'
      + '?email=' + email 
      + '&newEmail=' + newEmail
      + '&comment=' + comment ,{
        responseType:'json',
        observe: 'response' })
  }
  
login(email, password){
  console.log("login in service");
  return this.http.post(this._constant.uusUrl + '/ajax/admin/login'
      + '?email=' + email 
      + '&password=' + password ,{
        responseType:'json',
        observe: 'response' })
}
emailUpdateHistory(){
  console.log("login in service");
  return this.http.get(this._constant.uusUrl + '/ajax/admin/emailUpdateHistory'
  ,{
        responseType:'json',
        observe: 'response' })
}

logout(){
  console.log("logout in service");
  return this.http.get(this._constant.uusUrl + '/ajax/admin/logout'
  ,{
        responseType:'json',
        observe: 'response' })
}
}