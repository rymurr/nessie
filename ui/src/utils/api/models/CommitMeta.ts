/* tslint:disable */
/* eslint-disable */
/**
 * Nessie API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 0.8.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
/**
 * 
 * @export
 * @interface CommitMeta
 */
export interface CommitMeta {
    /**
     * 
     * @type {string}
     * @memberof CommitMeta
     */
    hash?: string;
    /**
     * 
     * @type {string}
     * @memberof CommitMeta
     */
    author?: string;
    /**
     * 
     * @type {Date}
     * @memberof CommitMeta
     */
    authorTime?: Date;
    /**
     * 
     * @type {Date}
     * @memberof CommitMeta
     */
    commitTime?: Date;
    /**
     * 
     * @type {string}
     * @memberof CommitMeta
     */
    committer?: string;
    /**
     * 
     * @type {string}
     * @memberof CommitMeta
     */
    message?: string;
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof CommitMeta
     */
    properties: { [key: string]: string; };
    /**
     * 
     * @type {string}
     * @memberof CommitMeta
     */
    signedOffBy?: string;
}

export function CommitMetaFromJSON(json: any): CommitMeta {
    return CommitMetaFromJSONTyped(json, false);
}

export function CommitMetaFromJSONTyped(json: any, ignoreDiscriminator: boolean): CommitMeta {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'hash': !exists(json, 'hash') ? undefined : json['hash'],
        'author': !exists(json, 'author') ? undefined : json['author'],
        'authorTime': !exists(json, 'authorTime') ? undefined : (new Date(json['authorTime'])),
        'commitTime': !exists(json, 'commitTime') ? undefined : (new Date(json['commitTime'])),
        'committer': !exists(json, 'committer') ? undefined : json['committer'],
        'message': !exists(json, 'message') ? undefined : json['message'],
        'properties': json['properties'],
        'signedOffBy': !exists(json, 'signedOffBy') ? undefined : json['signedOffBy'],
    };
}

export function CommitMetaToJSON(value?: CommitMeta | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'hash': value.hash,
        'author': value.author,
        'authorTime': value.authorTime === undefined ? undefined : (value.authorTime.toISOString()),
        'commitTime': value.commitTime === undefined ? undefined : (value.commitTime.toISOString()),
        'committer': value.committer,
        'message': value.message,
        'properties': value.properties,
        'signedOffBy': value.signedOffBy,
    };
}


