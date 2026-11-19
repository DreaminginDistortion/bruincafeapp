package com.bruincafe.app.data

/**
 * ============================================================================
 *  SUPABASE PROJECT DETAILS (already filled in for the Bruin Cafe Mobile app project)
 * ============================================================================
 * Supabase dashboard -> Project Settings -> API:
 *   url      = "Project URL"        e.g. "https://abcdxyz.supabase.co"
 *   anonKey  = "anon public" key    (safe to ship in an app IF Row Level Security
 *                                    is on -- see supabase/schema_and_seed.sql)
 *
 * NEVER paste the "service_role" key here; it bypasses all security.
 *
 * Leave both blank and the app runs entirely on MockData.kt.
 * ============================================================================
 */
object SupabaseConfig {
    val url: String = "https://soljrjnkffthzzugufmu.supabase.co"
    val anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNvbGpyam5rZmZ0aHp6dWd1Zm11Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk4NzkzNzUsImV4cCI6MjEwNTQ1NTM3NX0.KlHidb7MFawHZQOXYGnORegD19mkavn1476oJ-EiCm4"

    val isConfigured: Boolean get() = url.isNotBlank() && anonKey.isNotBlank()
}
