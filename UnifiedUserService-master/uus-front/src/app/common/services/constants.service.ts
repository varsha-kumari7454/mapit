import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ConstantsService {

  constructor() { }
  
  readonly ResponseStatus : object = { 
    INVALID_HASH : 722
  };
  readonly baseAppUrl: string = 'https://accounts.myanatomy.in/';
  readonly uusDistLocation: string = 'uus';
  readonly uusUrl: string = this.baseAppUrl + this.uusDistLocation;
}
