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
    //Dosyaları tek tek sırayla upload eder Aynı anda değil → sırayla gider. Tüm sonuçları bir array’e toplar
    return from(files).pipe(concatMap((file) => this.uploadFile(file)), toArray());
  }
}
