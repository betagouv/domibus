import {Component, Inject, ViewChild} from '@angular/core';
import {MatDialogRef} from '@angular/material';
import {TrustStoreService} from '../support/trustore.service';
import {AbstractControl, FormBuilder, FormControl, FormGroup, NgControl, NgForm, Validators} from '@angular/forms';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';

@Component({
  selector: 'app-truststore-upload',
  templateUrl: './truststore-upload.component.html',
  styleUrls: ['./truststore-upload.component.css'],
  providers: [TrustStoreService]
})
export class TrustStoreUploadComponent {

  truststoreForm: FormGroup;
  selectedFileName: string;
  fileSelected = false;

  @ViewChild('fileInput', {static: false}) fileInput;

  @ViewChild('passwordField', {static: false}) passwordField;

  constructor(public dialogRef: MatDialogRef<TrustStoreUploadComponent>, private fb: FormBuilder, @Inject(MAT_DIALOG_DATA) public data: any) {
    this.truststoreForm = fb.group({
      'password': new FormControl('', Validators.required),
      'allowChangingDiskStoreProps': new FormControl(false),
    });
  }

  public isFormValid(): boolean {
    return this.truststoreForm.valid && this.fileSelected;
  }

  public async submit() {
    if (!this.isFormValid()) {
      return;
    }
    const fileToUpload = this.fileInput.nativeElement.files[0];
    const password = this.truststoreForm.get('password').value;
    const allowChangingDiskStoreProps = this.truststoreForm.get('allowChangingDiskStoreProps').value;
    const result = {
      file: fileToUpload,
      password: password,
      allowChangingDiskStoreProps: allowChangingDiskStoreProps
    };
    this.dialogRef.close(result);
  }

  selectFile() {
    const fi = this.fileInput.nativeElement;
    const file = fi.files[0];
    this.selectedFileName = file.name;

    this.fileSelected = fi.files.length != 0;

    this.passwordField.nativeElement.focus();
  }

  public shouldShowErrors(field: NgControl | NgForm | AbstractControl): boolean {
    return (field.touched || field.dirty) && !!field.errors;
  }
}
