import axios from "axios";

import { API_URL } from "../config";

export const getTeams = () => {
    return axios.get(`${API_URL}/teams`);
};