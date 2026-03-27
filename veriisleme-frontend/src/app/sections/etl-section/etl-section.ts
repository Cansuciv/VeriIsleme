import { Component, inject, signal } from '@angular/core';

import { UploadService } from '../../services/upload.service';

type EtlMessageType = 'idle' | 'success' | 'error';

@Component({
  selector: 'app-etl-section',
  standalone: true,
  templateUrl: './etl-section.html',
  styleUrl: './etl-section.css'
})
export class EtlSectionComponent {
  private readonly uploadService = inject(UploadService);

  protected readonly etlMessage = signal(''); //ETL süreciyle ilgili kullanıcıya gösterilecek mesaj
  protected readonly etlMessageType = signal<EtlMessageType>('idle'); //idle | success | error → mesaj tipi
  protected readonly isEtlRunning = signal(false); //ETL sürecinin çalışıp çalışmadığını takip eder

  protected runEtl(): void { //Bu fonksiyon ETL’i başlatır ve durumu UI’ya yansıtır.
    this.isEtlRunning.set(true);
    this.etlMessage.set('ETL calisiyor...');
    this.etlMessageType.set('idle');

    this.uploadService.runEtl().subscribe({ //Backend çağrısı
      next: (response) => {
        this.isEtlRunning.set(false); //ETL başarılı → isEtlRunning = false
        this.etlMessage.set(response); //Backend’den gelen mesajı etlMessage’a set eder
        this.etlMessageType.set('success'); //ETL başarılı → etlMessageType = 'success'
      },
      error: (error) => {
        this.isEtlRunning.set(false);
        const message =
          typeof error?.error === 'string'
            ? error.error
            : error?.message ?? 'Hata: ETL baslatilamadi';
        this.etlMessage.set(message);
        this.etlMessageType.set('error');
      }
    });
  }
}
