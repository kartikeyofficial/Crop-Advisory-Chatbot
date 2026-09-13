import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChatResponse {
  reply: string;
}

export interface ChatMessage {
  role: string;
  content: string;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class CropService {
  private baseUrl = 'http://localhost:8080/api/crop';

  constructor(private http: HttpClient) {}

  async *sendMessageStream(sessionId: string, message: string) {
    const response = await fetch(`${this.baseUrl}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId, message })
    });

    if (!response.body) throw new Error('No response body');
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const chunk = decoder.decode(value, { stream: true });
      const lines = chunk.split('\n');
      for (const line of lines) {
        if (line.startsWith('data:')) {
          yield line.substring(5); // Remove 'data:'
        }
      }
    }
  }

  getHistory(sessionId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.baseUrl}/history/${sessionId}`);
  }
}
