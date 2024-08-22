/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Note } from './Note';
import type { NotebookSettings } from './NotebookSettings';
export type Notebook = {
    id: number;
    headNote: Note;
    notebookSettings: NotebookSettings;
    approvalStatus?: Notebook.approvalStatus;
    creatorId?: string;
};
export namespace Notebook {
    export enum approvalStatus {
        NOT_APPROVED = 'NOT_APPROVED',
        PENDING = 'PENDING',
        APPROVED = 'APPROVED',
    }
}

