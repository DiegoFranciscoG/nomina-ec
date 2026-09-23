import { Injectable } from '@angular/core';

export interface RuntimeConfig {
  apiUrl: string;
}

/**
 * Runtime configuration read from /config.json, so the same build can point to any API
 * (local, Docker behind nginx, or Render) without recompiling.
 */
@Injectable({ providedIn: 'root' })
export class ConfigService {
  private config: RuntimeConfig = { apiUrl: '' };

  async load(): Promise<void> {
    try {
      const response = await fetch('config.json', { cache: 'no-store' });
      if (response.ok) {
        const data = (await response.json()) as Partial<RuntimeConfig>;
        this.config = { apiUrl: (data.apiUrl ?? '').replace(/\/+$/, '') };
      }
    } catch {
      this.config = { apiUrl: '' };
    }
  }

  get apiUrl(): string {
    return this.config.apiUrl;
  }
}
