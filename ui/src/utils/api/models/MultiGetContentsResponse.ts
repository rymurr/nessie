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
import {
    ContentsWithKey,
    ContentsWithKeyFromJSON,
    ContentsWithKeyFromJSONTyped,
    ContentsWithKeyToJSON,
} from './';

/**
 * 
 * @export
 * @interface MultiGetContentsResponse
 */
export interface MultiGetContentsResponse {
    /**
     * 
     * @type {Array<ContentsWithKey>}
     * @memberof MultiGetContentsResponse
     */
    contents: Array<ContentsWithKey>;
}

export function MultiGetContentsResponseFromJSON(json: any): MultiGetContentsResponse {
    return MultiGetContentsResponseFromJSONTyped(json, false);
}

export function MultiGetContentsResponseFromJSONTyped(json: any, ignoreDiscriminator: boolean): MultiGetContentsResponse {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'contents': ((json['contents'] as Array<any>).map(ContentsWithKeyFromJSON)),
    };
}

export function MultiGetContentsResponseToJSON(value?: MultiGetContentsResponse | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'contents': ((value.contents as Array<any>).map(ContentsWithKeyToJSON)),
    };
}


