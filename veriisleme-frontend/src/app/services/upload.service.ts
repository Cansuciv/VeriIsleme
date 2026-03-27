import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { concatMap, from, Observable, toArray } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UploadService {
  private readonly apiBaseUrl = 'http://localhost:8080';

  constructor(private readonly http: HttpClient) {}

  uploadFile(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post(`${this.apiBaseUrl}/api/upload`, formData, {
      responseType: 'text'
    });
  }

  uploadFiles(files: File[]): Observable<string[]> {
    return from(files).pipe(concatMap((file) => this.uploadFile(file)), toArray());
  }

  runEtl(): Observable<string> {
    return this.http.post(`${this.apiBaseUrl}/api/etl/run`, null, {
      responseType: 'text'
    });
  }

  getSupersetGuestToken(dashboardId: string): Observable<string> {
    return this.http.get(`${this.apiBaseUrl}/api/superset/guest-token?dashboardId=${encodeURIComponent(dashboardId)}`, {
      responseType: 'text'
    });
  }
}
