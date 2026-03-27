import { Component, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';

import { UploadService } from '../../services/upload.service';

type StatusType = 'idle' | 'info' | 'progress' | 'success' | 'error';

@Component({
  selector: 'app-upload-section',
  standalone: true,
  templateUrl: './upload-section.html',
  styleUrl: './upload-section.css'
})
export class UploadSectionComponent {
  private readonly uploadService = inject(UploadService);

  protected readonly selectedFiles = signal<File[]>([]);
  protected readonly statusText = signal('Hazir. Dosya yukleyebilirsiniz.');
  protected readonly statusType = signal<StatusType>('idle');
  protected readonly isDragging = signal(false);
  protected readonly isUploading = signal(false);

  protected onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    if (!input?.files?.length) {
      return;
    }
    this.addFiles(input.files);
    input.value = '';
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
    if (!event.dataTransfer?.files?.length) {
      return;
    }
    this.addFiles(event.dataTransfer.files);
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(true);
  }

  protected onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
  }

  protected removeFile(index: number): void {
    const next = this.selectedFiles().filter((_, i) => i !== index);
    this.selectedFiles.set(next);
    this.statusType.set('info');
    this.statusText.set(next.length ? `${next.length} dosya secildi` : 'Dosyalar temizlendi');
  }

  protected clearFiles(): void {
    this.selectedFiles.set([]);
    this.statusType.set('info');
    this.statusText.set('Dosyalar temizlendi');
  }

  protected uploadFiles(): void {
    const files = this.selectedFiles();
    if (!files.length) {
      this.statusType.set('error');
      this.statusText.set('Once dosya secin.');
      return;
    }

    this.isUploading.set(true);
    this.statusType.set('progress');
    this.statusText.set(`Yukleme basladi (${files.length} dosya)...`);

    this.uploadService
      .uploadFiles(files)
      .pipe(finalize(() => this.isUploading.set(false)))
      .subscribe({
        next: () => {
          this.statusType.set('success');
          this.statusText.set(`Yukleme basarili (${files.length} dosya).`);
        },
        error: (error) => {
          const message =
            typeof error?.error === 'string'
              ? error.error
              : error?.message ?? 'Bilinmeyen hata';
          this.statusType.set('error');
          this.statusText.set(`Yukleme hatasi: ${message}`);
        }
      });
  }

  private addFiles(fileList: FileList): void {
    const incoming = Array.from(fileList);
    const existing = this.selectedFiles();
    const merged = [...existing];

    for (const file of incoming) {
      const alreadyAdded = merged.some(
        (item) =>
          item.name === file.name &&
          item.size === file.size &&
          item.lastModified === file.lastModified
      );
      if (!alreadyAdded) {
        merged.push(file);
      }
    }

    this.selectedFiles.set(merged);
    this.statusType.set('info');
    this.statusText.set(`${merged.length} dosya secildi`);
  }
}
