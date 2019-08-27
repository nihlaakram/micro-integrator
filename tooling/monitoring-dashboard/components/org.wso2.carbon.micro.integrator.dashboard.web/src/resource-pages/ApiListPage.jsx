/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React, {Component} from 'react';
import ListViewParent from '../common/ListViewParent';
import ResourceAPI from '../utils/apis/ResourceAPI';
import Link from '@material-ui/core/Link';

import MUIDataTable from "mui-datatables";

export default class ApiListPage extends Component {

    constructor(props) {
        super(props);
        this.apis = null;
        this.state = {
            data: [],
        };
    }

    /**
     * Retrieve apis from the MI.
     */
    componentDidMount() {
        this.retrieveApis();
    }

    retrieveApis() {
        const data = [];

        new ResourceAPI().getResourceList(`/apis`).then((response) => {
            this.apis = response.data.list || [];

            this.apis.forEach((element) => {
                const rowData = [];
                rowData.push(element.name);
                rowData.push(element.url);
                data.push(rowData);

            });
            this.setState({data: data});

        }).catch((error) => {
            //Handle errors here
        });
        console.log(this.state.data);
    }

    renderResourceList() {

        const columns = ["API Name", "URL",
            {
                name: "Action",
                options: {
                    customBodyRender: (value, tableMeta, updateValue) => {
                        return (
                            <Link component="button" variant="body2" onClick={() => {
                                this.props.history.push(`/api/sourceView?name=${tableMeta.rowData[0]}`)
                            }}>
                                Source View
                            </Link>
                        );
                    }
                }

            }];
        const options = {
            selectableRows: 'none'
        };

        return (
            <MUIDataTable
                title={"APIs"}
                data={this.state.data}
                columns={columns}
                options={options}
            />
        );
    }

    render() {
        return (
            <ListViewParent
                data={this.renderResourceList()}
            />
        );
    }
}