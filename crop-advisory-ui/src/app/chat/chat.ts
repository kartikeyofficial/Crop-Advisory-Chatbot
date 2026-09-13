import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CropService, ChatMessage } from '../crop';
import { marked } from 'marked';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.html',
  styleUrls: ['./chat.css']
})
export class Chat implements OnInit {
  sessionId = '';
  currentMessage = '';
  messages: ChatMessage[] = [];
  loading = false;

  constructor(private cropService: CropService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    // Generate a fresh session ID on every page load to start a new conversation
    this.sessionId = crypto.randomUUID();

    this.cropService.getHistory(this.sessionId).subscribe({
      next: (history) => (this.messages = history),
      error: () => {} // no history yet — fine for a new session
    });
  }

  async send() {
    const text = this.currentMessage.trim();
    if (!text) return;

    this.messages.push({ role: 'user', content: text, timestamp: '' });
    this.currentMessage = '';
    this.loading = true;

    // Create an empty assistant message to append the stream to
    const assistantMessage: ChatMessage = { role: 'assistant', content: '', timestamp: '' };
    // Do NOT push it yet, to prevent an empty bubble from appearing above "Thinking..."
    let isFirstChunk = true;

    try {
      const stream = this.cropService.sendMessageStream(this.sessionId, text);
      
      for await (const chunk of stream) {
        if (isFirstChunk) {
          this.messages.push(assistantMessage); // Push it only when the first word arrives
          this.loading = false;
          isFirstChunk = false;
        }
        assistantMessage.content += chunk;
        this.cdr.detectChanges(); // Force UI update immediately!
      }
    } catch (err) {
      if (isFirstChunk) {
        this.messages.push(assistantMessage);
      }
      assistantMessage.content = 'Something went wrong. Please try again.';
      this.cdr.detectChanges();
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }

  formatText(text: string): string {
    if (!text) return '';
    try {
      return marked.parse(text, { async: false }) as string;
    } catch (e) {
      return text;
    }
  }
}
