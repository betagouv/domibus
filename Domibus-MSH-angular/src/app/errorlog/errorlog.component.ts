﻿import {ChangeDetectorRef, Component, ElementRef, OnInit, Renderer2, TemplateRef, ViewChild} from '@angular/core';
import {Observable} from 'rxjs';
import {HttpClient, HttpParams} from '@angular/common/http';
import {ErrorLogResult} from './errorlogresult';
import {AlertService} from '../common/alert/alert.service';
import {ErrorlogDetailsComponent} from 'app/errorlog/errorlog-details/errorlog-details.component';
import {MatDialog, MatDialogRef} from '@angular/material';
import {ColumnPickerBase} from '../common/column-picker/column-picker-base';
import {RowLimiterBase} from '../common/row-limiter/row-limiter-base';
import {DownloadService} from '../common/download.service';
import {AlertComponent} from '../common/alert/alert.component';
import mix from '../common/mixins/mixin.utils';
import BaseListComponent from '../common/base-list.component';
import FilterableListMixin from '../common/mixins/filterable-list.mixin';
import SortableListMixin from '../common/mixins/sortable-list.mixin';
import {ServerPageableListMixin} from '../common/mixins/pageable-list.mixin';

@Component({
  moduleId: module.id,
  templateUrl: 'errorlog.component.html',
  providers: [],
  styleUrls: ['./errorlog.component.css']
})

export class ErrorLogComponent extends mix(BaseListComponent)
  .with(FilterableListMixin, SortableListMixin, ServerPageableListMixin)
  implements OnInit {

  dateFormat: String = 'yyyy-MM-dd HH:mm:ssZ';

  @ViewChild('rowWithDateFormatTpl', {static: false}) rowWithDateFormatTpl: TemplateRef<any>;

  timestampFromMaxDate: Date = new Date();
  timestampToMinDate: Date = null;
  timestampToMaxDate: Date = new Date();

  notifiedFromMaxDate: Date = new Date();
  notifiedToMinDate: Date = null;
  notifiedToMaxDate: Date = new Date();

  loading: boolean = false;

  mshRoles: string[];
  errorCodes: string[];

  advancedSearch: boolean;

  static readonly ERROR_LOG_URL: string = 'rest/errorlogs';
  static readonly ERROR_LOG_CSV_URL: string = ErrorLogComponent.ERROR_LOG_URL + '/csv?';

  constructor(private elementRef: ElementRef, private http: HttpClient, private alertService: AlertService,
              public dialog: MatDialog, private changeDetector: ChangeDetectorRef) {
    super();
  }

  ngOnInit() {
    super.ngOnInit();

    this['orderBy'] = 'timestamp';
    this['asc'] = false;

    this.search();
  }

  ngAfterViewInit() {
    this.columnPicker.allColumns = [
      {
        name: 'Signal Message Id',
        prop: 'errorSignalMessageId'
      },
      {
        name: 'AP Role',
        prop: 'mshRole',
        width: 50
      },
      {
        name: 'Message Id',
        prop: 'messageInErrorId',
      },
      {
        name: 'Error Code',
        width: 50
      },
      {
        name: 'Error Detail',
        width: 350
      },
      {
        cellTemplate: this.rowWithDateFormatTpl,
        name: 'Timestamp',
        width: 180
      },
      {
        cellTemplate: this.rowWithDateFormatTpl,
        name: 'Notified'
      }

    ];

    this.columnPicker.selectedColumns = this.columnPicker.allColumns.filter(col => {
      return ['Message Id', 'Error Code', 'Timestamp'].indexOf(col.name) != -1
    });
  }

  ngAfterViewChecked() {
    this.changeDetector.detectChanges();
  }

  createSearchParams(): HttpParams {
    let searchParams = new HttpParams();

    if (this.orderBy) {
      searchParams = searchParams.append('orderBy', this.orderBy);
    }
    if (this.asc != null) {
      searchParams = searchParams.append('asc', this.asc.toString());
    }

    if (this.activeFilter.errorSignalMessageId) {
      searchParams = searchParams.append('errorSignalMessageId', this.activeFilter.errorSignalMessageId);
    }
    if (this.activeFilter.mshRole) {
      searchParams = searchParams.append('mshRole', this.activeFilter.mshRole);
    }
    if (this.activeFilter.messageInErrorId) {
      searchParams = searchParams.append('messageInErrorId', this.activeFilter.messageInErrorId);
    }
    if (this.activeFilter.errorCode) {
      searchParams = searchParams.append('errorCode', this.activeFilter.errorCode);
    }
    if (this.activeFilter.errorDetail) {
      searchParams = searchParams.append('errorDetail', this.activeFilter.errorDetail);
    }
    if (this.activeFilter.timestampFrom != null) {
      searchParams = searchParams.append('timestampFrom', this.activeFilter.timestampFrom.getTime());
    }
    if (this.filter.timestampTo != null) {
      searchParams = searchParams.append('timestampTo', this.activeFilter.timestampTo.getTime());
    }
    if (this.activeFilter.notifiedFrom != null) {
      searchParams = searchParams.append('notifiedFrom', this.activeFilter.notifiedFrom.getTime());
    }
    if (this.activeFilter.notifiedTo != null) {
      searchParams = searchParams.append('notifiedTo', this.activeFilter.notifiedTo.getTime());
    }

    return searchParams;
  }

  getErrorLogEntries(): Promise<ErrorLogResult> {
    let searchParams = this.createSearchParams();

    searchParams = searchParams.append('page', this.offset.toString());
    searchParams = searchParams.append('pageSize', this.rowLimiter.pageSize.toString());

    return this.http.get<ErrorLogResult>(ErrorLogComponent.ERROR_LOG_URL, {params: searchParams})
      .toPromise();
  }

  page() {
    this.loading = true;
    this.getErrorLogEntries().then((result: ErrorLogResult) => {
      super.count = result.count;
      super.rows = result.errorLogEntries;

      if (result.filter.timestampFrom) {
        result.filter.timestampFrom = new Date(result.filter.timestampFrom);
      }
      if (result.filter.timestampTo) {
        result.filter.timestampTo = new Date(result.filter.timestampTo);
      }
      if (result.filter.notifiedFrom) {
        result.filter.notifiedFrom = new Date(result.filter.notifiedFrom);
      }
      if (result.filter.notifiedTo) {
        result.filter.notifiedTo = new Date(result.filter.notifiedTo);
      }

      this['filter'] = result.filter;
      this.mshRoles = result.mshRoles;
      this.errorCodes = result.errorCodes;

      this.loading = false;
    }, (error: any) => {
      this.loading = false;
      this.alertService.exception('Error occured:', error);
    });

  }

  // onPage(event) {
  //   super.resetFilters();
  //   super.offset = event.offset;
  //   this.page();
  // }

  /**
   * The method is an override of the abstract method defined in SortableList mixin
   */
  // public reload() {
  //   super.offset = 0;
  //   this.page();
  // }

  /**
   * The method is an override of the abstract method defined in SortableList mixin
   */
  public onBeforeSort() {
    super.resetFilters();
  }

  // changePageSize(newPageLimit: number) {
  //   super.resetFilters();
  //   this.offset = 0;
  //   this.rowLimiter.pageSize = newPageLimit;
  //   this.page();
  // }

  search() {
    super.offset = 0;
    this.page();
  }

  onTimestampFromChange(event) {
    this.timestampToMinDate = event.value;
  }

  onTimestampToChange(event) {
    this.timestampFromMaxDate = event.value;
  }

  onNotifiedFromChange(event) {
    this.notifiedToMinDate = event.value;
  }

  onNotifiedToChange(event) {
    this.notifiedFromMaxDate = event.value;
  }

  toggleAdvancedSearch(): boolean {
    this.advancedSearch = !this.advancedSearch;
    return false;//to prevent default navigation
  }

  onActivate(event) {
    if ('dblclick' === event.type) {
      this.details(event.row);
    }
  }

  details(selectedRow: any) {
    let dialogRef: MatDialogRef<ErrorlogDetailsComponent> = this.dialog.open(ErrorlogDetailsComponent);
    dialogRef.componentInstance.message = selectedRow;
    // dialogRef.componentInstance.currentSearchSelectedSource = this.currentSearchSelectedSource;
    dialogRef.afterClosed().subscribe(result => {
      //Todo:
    });
  }

  public get csvUrl(): string {
    return ErrorLogComponent.ERROR_LOG_CSV_URL + this.createSearchParams().toString();
  }
}
