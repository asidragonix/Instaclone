-- =========================================================
-- Supabase Table Setup for InstaClone Appointment Bookings
-- Project ID: kilkdijwewzoofuppvck
-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/kilkdijwewzoofuppvck/sql/new
-- =========================================================

-- 1. Enable standard UUID extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Create the appointments table
CREATE TABLE IF NOT EXISTS public.appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT,
    appointment_type TEXT NOT NULL DEFAULT 'General Consultation',
    appointment_date TEXT NOT NULL,
    appointment_time TEXT NOT NULL,
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. Enable Row Level Security (RLS)
ALTER TABLE public.appointments ENABLE ROW LEVEL SECURITY;

-- 4. Clean up any previous policy to prevent "already exists" errors
DROP POLICY IF EXISTS "Enable all access for anon and authenticated" ON public.appointments;
DROP POLICY IF EXISTS "Allow public insert on appointments" ON public.appointments;
DROP POLICY IF EXISTS "Allow public select on appointments" ON public.appointments;
DROP POLICY IF EXISTS "Allow public insert" ON public.appointments;
DROP POLICY IF EXISTS "Allow public select" ON public.appointments;

-- 5. Create RLS Policy allowing app visitors (anon) & users to book and view appointments
CREATE POLICY "Enable all access for anon and authenticated"
ON public.appointments
FOR ALL
TO anon, authenticated
USING (true)
WITH CHECK (true);

-- 6. Grant access to API roles
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON TABLE public.appointments TO anon, authenticated, service_role;

-- 7. Add index for fast query sorting
CREATE INDEX IF NOT EXISTS idx_appointments_created_at ON public.appointments (created_at DESC);
