/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.trebuchet.preference;

import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.cyanogenmod.trebuchet.AppsCustomizePagedView;
import com.cyanogenmod.trebuchet.Workspace;

public final class PreferencesProvider {
    
    public static boolean USE_MIRROR_VIEW = true;
    
    public static final boolean USE_GL_MIRROR   = true;
    
    public static final String PREFERENCES_KEY = "com.aetherar.launcher_preferences";

    public static final String PREFERENCES_CHANGED = "preferences_changed";

    private static Map<String, Object> sKeyValues;
    
    private static boolean sMirrorEnabled;
    
    private static SharedPreferences sPreferences = null;
    
    @SuppressWarnings("unchecked")
    public static void load(Context context) {
    	sPreferences = context.getApplicationContext().getSharedPreferences(PREFERENCES_KEY, 0);
        sKeyValues = (Map<String, Object>)sPreferences.getAll();
    }
    
    public static boolean isMirrorEnabled() {
        return USE_MIRROR_VIEW && sMirrorEnabled;
    }
    
    public static void setMirrorEnabled(boolean enabled) {
        sMirrorEnabled = enabled;
    }

    private static void setInt(String key, int value) {
    	sKeyValues.put(key, value);
    	Editor edit = sPreferences.edit();
    	edit.putInt(key, value);
    	edit.commit();
    }
    
    private static int getInt(String key, int def) {
        return sKeyValues.containsKey(key) && sKeyValues.get(key) instanceof Integer ?
                (Integer) sKeyValues.get(key) : def;
    }

    private static boolean getBoolean(String key, boolean def) {
        return sKeyValues.containsKey(key) && sKeyValues.get(key) instanceof Boolean ?
                (Boolean) sKeyValues.get(key) : def;
    }

    private static void setBoolean(Context ctx, String key, boolean value) {
        SharedPreferences preferences = ctx.getSharedPreferences(PREFERENCES_KEY, 0);
        Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.apply(); // For better performance
        sKeyValues.put(key, Boolean.valueOf(value));
    }

    private static String getString(String key, String def) {
        return sKeyValues.containsKey(key) && sKeyValues.get(key) instanceof String ?
                (String) sKeyValues.get(key) : def;
    }

    public static class Interface {
        public static class Homescreen {
            public static int getNumberHomescreens() {
                return getInt("ui_homescreen_screens", 5);
            }
            public static int getDefaultHomescreen(int def) {
                return getInt("ui_homescreen_default_screen", def + 1) - 1;
            }
            public static int getCellCountX(int def) {
                String[] values = getString("ui_homescreen_grid", "0|" + def).split("\\|");
                try {
                    return Integer.parseInt(values[1]);
                } catch (NumberFormatException e) {
                    return def;
                }
            }
            public static int getCellCountY(int def) {
                String[] values = getString("ui_homescreen_grid", def + "|0").split("\\|");
                try {
                    return Integer.parseInt(values[0]);
                } catch (NumberFormatException e) {
                    return def;
                }
            }
            public static boolean getStretchScreens() {
                return getBoolean("ui_homescreen_stretch_screens", true);
            }
            public static boolean getShowSearchBar() {
                return getBoolean("ui_homescreen_general_search", true);
            }
            public static boolean getHideIconLabels() {
                return getBoolean("ui_homescreen_general_hide_icon_labels", false);
            }
            public static class Scrolling {
                public static Workspace.TransitionEffect getTransitionEffect(String def) {
                    try {
                        return Workspace.TransitionEffect.valueOf(
                                getString("ui_homescreen_scrolling_transition_effect", def));
                    } catch (IllegalArgumentException iae) {
                        // Continue
                    }

                    try {
                        return Workspace.TransitionEffect.valueOf(def);
                    } catch (IllegalArgumentException iae) {
                        // Continue
                    }

                    return Workspace.TransitionEffect.Standard;
                }
                public static boolean getScrollWallpaper() {
                    return getBoolean("ui_homescreen_scrolling_scroll_wallpaper", true);
                }
                public static boolean getWallpaperHack(boolean def) {
                    return getBoolean("ui_homescreen_scrolling_wallpaper_hack", def);
                }
                public static int getWallpaperSize() {
                    return getInt("ui_homescreen_scrolling_wallpaper_size", 2);
                }
                public static boolean getFadeInAdjacentScreens(boolean def) {
                    return getBoolean("ui_homescreen_scrolling_fade_adjacent_screens", def);
                }
                public static boolean getShowOutlines(boolean def) {
                    return getBoolean("ui_homescreen_scrolling_show_outlines", def);
                }
            }
            public static class Indicator {
                public static boolean getShowScrollingIndicator() {
                    return getBoolean("ui_homescreen_indicator_enable", true);
                }
                public static boolean getFadeScrollingIndicator() {
                    return getBoolean("ui_homescreen_indicator_fade", true);
                }
                public static int getScrollingIndicatorPosition() {
                    return Integer.parseInt(getString("ui_homescreen_indicator_position", "0"));
                }
            }
        }

        public static class Drawer {
            public static boolean getVertical() {
                return getString("ui_drawer_orientation", "horizontal").equals("horizontal");
            }
            public static String getHiddenApps() {
                return getString("ui_drawer_hidden_apps", "");
            }
            public static boolean getRemoveShortcutsOfHiddenApps() {
                return getBoolean("ui_drawer_remove_hidden_apps_shortcuts", true);
            }
            public static boolean getRemoveWidgetsOfHiddenApps() {
                return getBoolean("ui_drawer_remove_hidden_apps_widgets", true);
            }
            public static boolean getJoinWidgetsApps() {
                return getBoolean("ui_drawer_widgets_join_apps", true);
            }
            public static class Scrolling {
                public static AppsCustomizePagedView.TransitionEffect getTransitionEffect(String def) {
                    try {
                        return AppsCustomizePagedView.TransitionEffect.valueOf(
                                getString("ui_drawer_scrolling_transition_effect", def));
                    } catch (IllegalArgumentException iae) {
                        // Continue
                    }

                    try {
                        return AppsCustomizePagedView.TransitionEffect.valueOf(def);
                    } catch (IllegalArgumentException iae) {
                        // Continue
                    }

                    return AppsCustomizePagedView.TransitionEffect.Standard;
                }
                public static boolean getFadeInAdjacentScreens() {
                    return getBoolean("ui_drawer_scrolling_fade_adjacent_screens", false);
                }
            }
            public static class Indicator {
                public static boolean getShowScrollingIndicator() {
                    return getBoolean("ui_drawer_indicator_enable", true);
                }
                public static boolean getFadeScrollingIndicator() {
                    return getBoolean("ui_drawer_indicator_fade", true);
                }
                public static int getScrollingIndicatorPosition() {
                    return Integer.parseInt(getString("ui_drawer_indicator_position", "0"));
                }
            }
        }

        public static class Dock {
            public static boolean getShowDock() {
                return getBoolean("ui_dock_enabled", true);
            }
            public static int getNumberPages() {
                return getInt("ui_dock_pages", 1);
            }
            public static int getDefaultPage(int def) {
                return getInt("ui_dock_default_page", def + 1) - 1;
            }
            public static int getNumberIcons(int def) {
                return getInt("ui_dock_icons", def);
            }
            public static boolean getHideIconLabels() {
                return getBoolean("ui_dock_hide_icon_labels", true);
            }
            public static int getIconScale(int def) {
                return getInt("ui_dock_icon_scale", def);
            }
            public static boolean getShowDivider() {
                return getBoolean("ui_dock_divider", true);
            }
        }
        
        public static class ThreeD {
            public static int getZScale(int def) {
                return getInt("ui_3d_z_scale", def);
            }        	
            public static void setZScale(int value) {
            	setInt("ui_3d_z_scale", value);            	
            }
            public static int getZDistance(int def) {
                return getInt("ui_3d_cam_distance", def);
            }           
            public static int getShaderType(int def) {
                return Integer.parseInt(getString("ui_3d_shader_type", Integer.toString(def)));
            }     
            public static int getSensorResetAcceleration(int def) {
                return Integer.parseInt(getString("ui_3d_sensor_reset_acceleration", Integer.toString(def)));                
            }
            public static boolean getShowFPS(boolean def) {
            	return getBoolean("ui_3d_show_fps", def);
            }
        }

        public static class Icons {

        }

        public static class General {
            public static boolean getAutoRotate(boolean def) {
                return getBoolean("ui_general_orientation", def);
            }
            public static boolean getLockWorkspace(boolean def) {
                return getBoolean("ui_general_lock_workspace", def);
            }
            public static void setLockWorkspace(Context ctx, boolean value) {
                setBoolean(ctx, "ui_general_lock_workspace", value);
            }
            public static boolean getFullscreenMode() {
                return getBoolean("ui_general_fullscreen", false);
            }
        }
    }

    public static class Application {
    	public static void setPopupDueCount(int count) {
            setInt("app_popup_due_count", count);
        }
    	
    	public static int getPopupDueCount(int count) {
            return getInt("app_popup_due_count", count);
        }
    	
    	// 0 not shown
    	// 1 Don't show
    	// 2 later
    	// 3 already shown
    	public static void setPopupAction(int action) {
            setInt("app_popup_action", action);
        }
    	
    	public static int getPopupAction(int action) {
            return getInt("app_popup_action", action);
        }
    }
}
