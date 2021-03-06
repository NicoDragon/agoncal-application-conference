import { Component, OnInit } from '@angular/core';
import { Location } from '@angular/common';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { DefaultApiSchedule, DefaultApiSpeaker, DefaultApiTalk } from '../ms/api/api';
import * as models from '../ms/model/models';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { AuthService } from '../auth.service';
import 'rxjs/Rx';
import { Http, Headers, RequestOptionsArgs, Response, URLSearchParams } from '@angular/http';

@Component({
  selector: 'app-vote',
  templateUrl: './vote.component.html',
  styleUrls: ['./vote.component.css']
})
export class VoteComponent implements OnInit {

  private id: string;
  public session: models.Session;
  public talk: models.Talk;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private scheduleService: DefaultApiSchedule,
    private talkService: DefaultApiTalk,
    private speakerService: DefaultApiSpeaker,
    private authService: AuthService,
    private http: Http

  ) { }

  rating: any;

  ngOnInit() {
    this.route.params.forEach((params: Params) => {
      this.id = params['id'];
    });

    this.getSession(this.id);

  }



  onSubmit() {

    var jwt = this.authService.jwt;
    let defaultHeaders: Headers = new Headers();
    defaultHeaders.append('Authorization', 'Bearer ' + jwt);

    let requestOptions: RequestOptionsArgs = {
      method: 'POST',
      headers: defaultHeaders
    };

    return this.http.request('http://conference.docker.localhost:90/conference-rating/api/ratings/uni_room9_tuesday_8_13h30_16h30',
      requestOptions)
      .subscribe((response:Response) => {
        console.log(response.json());
      })
      

  }

  private getSession(id: string) {
    this.scheduleService
      .retrieve(id)
      .flatMap((s: models.Session) => {
        this.session = s;
        return s ? this.talkService.retrieve(s.talk.id) : undefined;
      }).subscribe((t: models.Talk) => {
        this.talk = t;
        return t ? t.speakers : undefined;
      });
  }

}
