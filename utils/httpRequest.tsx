import React, { Component } from 'react';
export const httpRequest = (url: string, method: string,body:string) =>{
    const baseUrl = `http://jsonplaceholder.typicode.com${url}`
    fetch(baseUrl, {
        method: method,
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
        },
        body:body
    }).then((response) => response.json()).then((result) => {
        console.log(result)
    })
}