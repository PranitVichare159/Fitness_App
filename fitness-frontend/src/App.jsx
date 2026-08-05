import { Box, Button } from "@mui/material"
import { useContext, useEffect, useState } from 'react'
import { AuthContext } from "react-oauth2-code-pkce"
import { useDispatch } from "react-redux";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router";
import { logout, setCredentials } from "./store/authSlice";
import ActivityDetail from "./components/ActivityDetail";
import ActivityList from "./components/ActivityList";
import ActivityForm from "./components/ActivityForm";

const ActivitiesPage = () => {
  return (
    <Box sx={{ p: 2, border: '1px dashed grey' }}>
      <ActivityForm onActivityAdded={() => window.location.reload()} />
      <ActivityList />
    </Box>
  );
}

function App() {
  const { token, tokenData, logIn, logOut } = useContext(AuthContext);
  const dispatch = useDispatch();
  const [authReady, setAuthReady] = useState(false);

  useEffect(() => {
    if (token) {
      // ✅ Fix 1: Save to localStorage so api interceptor can read them
      localStorage.setItem('token', token);
      localStorage.setItem('userId', tokenData?.sub || tokenData?.id || '');

      dispatch(setCredentials({ token, user: tokenData }));
      setAuthReady(true);
    }
  }, [token, tokenData, dispatch]);

  const handleLogout = () => {
    // ✅ Fix 2: Clear localStorage on logout
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    dispatch(logout());
    logOut();
  };

  return (
    <Router>
      {!token ? (
        <Button variant="contained" onClick={() => logIn()}>
          LOGIN
        </Button>
      ) : (
        <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
          <Button variant="contained" onClick={handleLogout}>
            LOGOUT
          </Button>

          <Routes>
            <Route path="/activities" element={<ActivitiesPage />} />
            <Route path="/activities/:id" element={<ActivityDetail />} />
            <Route path="/" element={<Navigate to="/activities" replace />} />
          </Routes>
        </Box>
      )}
    </Router>
  )
}

export default App