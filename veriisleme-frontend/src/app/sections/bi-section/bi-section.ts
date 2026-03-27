import { Component, ElementRef, ViewChild, signal } from '@angular/core';
import { embedDashboard } from '@superset-ui/embedded-sdk';

import { UploadService } from '../../services/upload.service';

@Component({
  selector: 'app-bi-section',
  standalone: true,
  templateUrl: './bi-section.html',
  styleUrl: './bi-section.css'
})
export class BiSectionComponent {
  protected readonly supersetMessage = signal('');
  protected readonly supersetDashboardId = signal('47de32e0-28cb-4d80-b563-9012e28dccf8'); 
  // 47de32e0-28cb-4d80-b563-9012e28dccf8 --> http://localhost:8088 portu için deneme
  // 3daf2ab0-374b-484e-8c59-2bddd705773d --> http://localhost:8089 portu için deneme
 
  @ViewChild('supersetMount', { static: false }) protected supersetMount?: ElementRef<HTMLDivElement>; //Bu div’in içine Superset dashboard embed edilecek

  constructor(private readonly uploadService: UploadService) {}

  protected embedSuperset(): void { //Bu fonksiyon Superset dashboard’u embed eder ve UI durumunu yönetir.
    const dashboardId = this.normalizeDashboardId(this.supersetDashboardId()); //normalizeDashboardId: URL veya ID’den temiz ve doğru ID’yi çıkarır. Eğer ID veya mount point yoksa hata mesajı set edilir
    if (!dashboardId) {
      this.supersetMessage.set('Dashboard ID gerekli.');
      return;
    }
    if (!this.supersetMount?.nativeElement) {
      this.supersetMessage.set('Gomulecek alan bulunamadi.');
      return;
    }

    this.supersetMessage.set('Superset yukleniyor...');
    const mountPoint = this.supersetMount.nativeElement;
    mountPoint.innerHTML = '';

    //Guest token ile Superset embed etme işlemi başlatılır. Token alındıktan sonra embedDashboard fonksiyonu çağrılır.
    //Embed başarılı olursa mesaj temizlenir, hata olursa mesaj set edilir.
    this.uploadService.getSupersetGuestToken(dashboardId).subscribe({
      next: async (token) => {
        try {
          await embedDashboard({
            id: dashboardId,
            supersetDomain: 'http://localhost:8088',
            mountPoint,
            fetchGuestToken: () => Promise.resolve(token),
            dashboardUiConfig: {
              hideTitle: true,
              filters: {
                expanded: true
              }
            },
            iframeSandboxExtras: ['allow-top-navigation', 'allow-popups-to-escape-sandbox']
          });
          const iframe = mountPoint.querySelector('iframe');
          if (iframe) {
            iframe.style.width = '100%';
            iframe.style.height = '100%';
            iframe.style.minHeight = '600px';
            iframe.style.border = '0';
          }
          this.supersetMessage.set('');
        } catch (e: any) {
          this.supersetMessage.set(e?.message ?? 'Superset gomulemedi.');
        }
      },
      error: (error) => {
        const message =
          typeof error?.error === 'string'
            ? error.error
            : error?.message ?? 'Superset token alinamadi';
        this.supersetMessage.set(message);
      }
    });
  }

  //URL’den sadece dashboard ID çıkarılır. Eğer input boşsa veya geçerli bir ID bulunamazsa boş string döner.
  private normalizeDashboardId(input: string): string { 
    const raw = input.trim();
    if (!raw) {
      return '';
    }
    const withoutQuery = raw.split('?')[0].split('#')[0];
    const marker = '/superset/dashboard/';
    const idx = withoutQuery.indexOf(marker);
    if (idx >= 0) {
      const rest = withoutQuery.slice(idx + marker.length);
      const parts = rest.split('/').filter(Boolean);
      return parts[0] ?? '';
    }
    const parts = withoutQuery.split('/').filter(Boolean);
    return parts[parts.length - 1] ?? '';
  }
}
