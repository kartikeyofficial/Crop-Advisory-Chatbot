import { TestBed } from '@angular/core/testing';
import { Crop } from './crop';

describe('Crop', () => {
  let service: Crop;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Crop);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
