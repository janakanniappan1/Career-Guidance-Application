package com.example

import android.app.Application
import android.util.Log
import com.example.data.pdf.PdfExtractorImpl
import com.example.data.remote.SupabaseClientProvider

class CgaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("CgaApplication", "Initializing CGA Application, Supabase, and PDFBox")
        SupabaseClientProvider.init(this)
        PdfExtractorImpl.ensureInitialized(this)
    }
}
