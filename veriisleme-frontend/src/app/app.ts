import { Component } from '@angular/core';

import { UploadSectionComponent } from './sections/upload-section/upload-section';
import { EtlSectionComponent } from './sections/etl-section/etl-section';
import { BiSectionComponent } from './sections/bi-section/bi-section';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [UploadSectionComponent, EtlSectionComponent, BiSectionComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}
