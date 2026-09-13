import { Component } from '@angular/core';
import { Chat } from './chat/chat';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [Chat],
  template: '<app-chat></app-chat>',
  styles: ['']
})
export class App {
  title = 'crop-advisory-ui';
}
