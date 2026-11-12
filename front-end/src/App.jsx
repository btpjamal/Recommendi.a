import { Routes, Route, Navigate } from "react-router";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Profile from "./pages/Profile";
import Recommendations from "./pages/Recommendations";
import RecommendationDetails from "./pages/RecommendationDetails.jsx";
import Onboarding from "./pages/Onboarding";
import GeneralPlan from "./pages/GeneralPlan.jsx";
import MyProfile from "./pages/MyProfile";
import SocialProfile from "./pages/SocialProfile";
import Community from "./pages/Community";
import CommunityDetails from "./pages/CommunityDetails";
import PrivateConversation from "./pages/PrivateConversation";
import AISettings from "./pages/AISettings";
import AdminRoute from "./components/AdminRoute";
import AdminDashboard from "./pages/AdminDashboard";
import AdminLayout from "./components/AdminLayout";
import AdminSuggestions from "./pages/AdminSuggestions";
import SuggestHobby from "./pages/SuggestHobby";
import MySuggestions from "./pages/MySuggestions";
import AdminHobbies from "./pages/AdminHobbies";
import AdminCommunityReports from "./pages/AdminCommunityReports.jsx";
import AdminLocationRules from "./pages/AdminLocationRules";

export default function App() {
  return (
      <Routes>
        <Route path="/" element={<Navigate to="/login" />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route element={<ProtectedRoute />}>
        <Route path="/profile" element={<Profile />} />
        <Route path="/interests" element={<Navigate to="/myProfile" replace />} />
        <Route path="/recommendations" element={<Recommendations />} />
        <Route path="/feedbacks" element={<Navigate to="/recommendations?aba=interessados" replace />} />
        <Route path="/recommendations/:hobbyId" element={<RecommendationDetails />} />
        <Route path="/onboarding" element={<Onboarding />} />
        <Route path="/general-plan" element={<GeneralPlan />} />
        <Route path="/myProfile" element={<MyProfile />} />
        <Route path="/community/profile" element={<SocialProfile />} />
        <Route path="/community/users/:userId" element={<SocialProfile />} />
        <Route path="/community" element={<Community />} />
        <Route path="/community/communities/:communityId" element={<CommunityDetails />} />
        <Route path="/community/conversations/:contactId" element={<PrivateConversation />} />
        <Route path="/ai-settings" element={<AISettings />} />
        <Route path="/suggest-hobby" element={<SuggestHobby />} />
        <Route path="/my-suggestions" element={<MySuggestions />} />
        </Route>

            <Route element={<AdminRoute />}>
                  <Route path="/admin" element={<AdminLayout />}>
                        <Route index element={<AdminDashboard />} />

                        <Route
                            path="suggestions"
                            element={<AdminSuggestions />}
                        />

                        <Route path="community-reports" element={<AdminCommunityReports />} />
                        <Route path="hobbies" element={<AdminHobbies />} />

                        <Route
                            path="location-rules"
                            element={<AdminLocationRules />}
                        />

                  </Route>
            </Route>


      </Routes>
  );
}
