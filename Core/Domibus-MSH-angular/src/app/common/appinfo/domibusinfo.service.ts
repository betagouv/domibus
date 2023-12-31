import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import 'rxjs/add/operator/map';
import {DomibusInfo} from './domibusinfo';
import {SupportTeamInfo} from "../../security/not-authorized/supportteaminfo";

@Injectable()
export class DomibusInfoService {

  private isExtAuthProviderEnabledPromise: Promise<boolean>;
  private domibusInfo: Promise<DomibusInfo>;
  private supportTeamInfo: Promise<SupportTeamInfo>;

  constructor(private http: HttpClient) {
  }

  getDomibusInfo(): Promise<DomibusInfo> {
    if (!this.domibusInfo) {
      this.domibusInfo = this.http.get<DomibusInfo>('rest/application/info').toPromise();
    }
    return this.domibusInfo;
  }

  isExtAuthProviderEnabled(): Promise<boolean> {
    if (!this.isExtAuthProviderEnabledPromise) {
      this.isExtAuthProviderEnabledPromise = this.http.get<boolean>('rest/application/extauthproviderenabled').toPromise();
    }
    return this.isExtAuthProviderEnabledPromise;
  }

  getSupportTeamInfo(): Promise<SupportTeamInfo> {
    if (!this.supportTeamInfo) {
      this.supportTeamInfo = this.http.get<SupportTeamInfo>('rest/application/supportteam').toPromise();
    }
    return this.supportTeamInfo;
  }
}
