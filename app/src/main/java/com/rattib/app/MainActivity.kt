package com.rattib.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val bg = Color.rgb(9,9,11)
    private val panel = Color.rgb(20,20,24)
    private val gold = Color.rgb(214,179,106)
    private val white = Color.rgb(246,246,247)
    private val muted = Color.rgb(165,165,175)
    private lateinit var root: LinearLayout
    private var tasks = mutableListOf<TaskItem>()
    private var selectedDate = Calendar.getInstance()
    private var selectedHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    private var selectedMinute = (Calendar.getInstance().get(Calendar.MINUTE) + 2) % 60
    private var repeatChoice = "none"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg; window.navigationBarColor = bg
        tasks = TaskStore.load(this)
        askNotificationPermission()
        render()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 44)
        }
    }

    private fun text(value: String, size: Float, color: Int = white, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = value; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.RIGHT
        }

    private fun button(value: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = value; setTextColor(Color.rgb(20,16,8)); textSize = 14f
            setBackgroundColor(gold); setOnClickListener { onClick() }
            isAllCaps = false
        }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(bg)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(18), dp(18), dp(18), dp(22))
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        root.addView(text("رَتّب  ✦", 29f, gold, true), lp(-1, -2, 0, 4, 0, 0))
        root.addView(text("منظّم يومك • أسود وذهبي", 13f, muted), lp(-1,-2,0,0,0,18))
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16),dp(16),dp(16),dp(16)); setBackgroundColor(Color.rgb(30,27,20))
        }
        hero.addView(text("خلّ يومك يمشي على خطتك.", 22f, white, true))
        hero.addView(text("أضف مهامك وحدد وقت التنبيه لكل واحدة.", 13f, muted), lp(-1,-2,0,8,0,0))
        root.addView(hero, lp(-1,-2,0,0,0,14))
        root.addView(text("＋ إضافة مهمة", 19f, gold, true), lp(-1,-2,0,0,0,8))
        val titleInput = EditText(this).apply {
            hint = "اسم المهمة، مثل: مذاكرة الرياضيات"
            setHintTextColor(muted); setTextColor(white); textSize=16f
            setBackgroundColor(panel); setPadding(dp(12),dp(10),dp(12),dp(10))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        root.addView(titleInput, lp(-1,dp(52),0,0,0,10))
        // Date and time are selected from native Android dialogs.
        val dateLabel = text("التاريخ: " + dateFmt(selectedDate.time), 14f, white)
        dateLabel.setPadding(dp(12),dp(12),dp(12),dp(12)); dateLabel.setBackgroundColor(panel)
        dateLabel.setOnClickListener { DatePickerDialog(this,{_,y,m,d-> selectedDate.set(y,m,d); dateLabel.text="التاريخ: "+dateFmt(selectedDate.time) },selectedDate.get(Calendar.YEAR),selectedDate.get(Calendar.MONTH),selectedDate.get(Calendar.DAY_OF_MONTH)).show() }
        root.addView(dateLabel, lp(-1,-2,0,0,0,8))
        val timeLabel = text(String.format(Locale.getDefault(),"وقت التنبيه: %02d:%02d",selectedHour,selectedMinute),14f,white)
        timeLabel.setPadding(dp(12),dp(12),dp(12),dp(12)); timeLabel.setBackgroundColor(panel)
        timeLabel.setOnClickListener { TimePickerDialog(this,{_,h,m->selectedHour=h;selectedMinute=m;timeLabel.text=String.format(Locale.getDefault(),"وقت التنبيه: %02d:%02d",h,m)},selectedHour,selectedMinute,true).show() }
        root.addView(timeLabel, lp(-1,-2,0,0,0,8))
        val repeatSpinner = Spinner(this)
        val options = listOf("بدون تكرار","يوميًا","أسبوعيًا")
        repeatSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        repeatSpinner.setBackgroundColor(panel)
        repeatSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                repeatChoice = listOf("none","daily","weekly")[position]
            }
        }
        root.addView(repeatSpinner, lp(-1,dp(48),0,0,0,10))
        root.addView(button("＋ حفظ المهمة وتفعيل المنبّه") {
            val title = titleInput.text.toString().trim()
            if (title.isEmpty()) { titleInput.error = "اكتب اسم المهمة"; return@button }
            val c = selectedDate.clone() as Calendar
            c.set(Calendar.HOUR_OF_DAY,selectedHour); c.set(Calendar.MINUTE,selectedMinute); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0)
            var trigger = c.timeInMillis
            if (repeatChoice != "none" && trigger <= System.currentTimeMillis()) {
                val step = if (repeatChoice == "daily") 86400000L else 604800000L
                while (trigger <= System.currentTimeMillis()) trigger += step
            }
            if (trigger <= System.currentTimeMillis()) {
                AlertDialog.Builder(this).setTitle("وقت التنبيه مضى").setMessage("اختر وقتًا في المستقبل لهذه المهمة.").setPositiveButton("حسنًا",null).show()
                return@button
            }
            val task = TaskItem(System.currentTimeMillis(), title, trigger, repeatChoice, false)
            tasks.add(task); TaskStore.save(this,tasks); AlarmScheduler.schedule(this,task)
            Toast.makeText(this,"تم حفظ المهمة وضبط التنبيه ✓",Toast.LENGTH_LONG).show()
            render()
        })
        root.addView(text("مهامك",20f,gold,true),lp(-1,-2,0,20,0,10))
        val ordered = tasks.sortedBy { it.triggerAt }
        if (ordered.isEmpty()) root.addView(text("ما عندك مهام حتى الآن. أضف أول مهمة وابدأ! ✨",14f,muted),lp(-1,-2,0,4,0,8))
        ordered.forEach { task ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(13),dp(12),dp(13),dp(12)); setBackgroundColor(panel)
            }
            card.addView(text((if(task.done) "✓  " else "◷  ")+task.title,17f,if(task.done) muted else white,true))
            card.addView(text(dateTimeFmt(task.triggerAt) + "  •  " + when(task.repeat){"daily"->"يوميًا";"weekly"->"أسبوعيًا";else->"مرة واحدة"},12f,muted),lp(-1,-2,0,6,0,0))
            val row = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.RIGHT }
            row.addView(button(if(task.done) "إلغاء الإنجاز" else "تمت") {
                task.done = !task.done
                if(task.done) AlarmScheduler.cancel(this,task) else AlarmScheduler.schedule(this,task)
                TaskStore.save(this,tasks); render()
            })
            row.addView(button("حذف") {
                AlertDialog.Builder(this).setMessage("حذف المهمة؟").setNegativeButton("إلغاء",null).setPositiveButton("حذف"){_,_->AlarmScheduler.cancel(this,task);tasks.removeAll{it.id==task.id};TaskStore.save(this,tasks);render()}.show()
            },lp(-2,-2,8,0,0,0))
            card.addView(row,lp(-1,-2,0,10,0,0))
            root.addView(card,lp(-1,-2,0,0,0,10))
        }
        root.addView(button("السماح بالتنبيهات / إعدادات المنبه") {
            askNotificationPermission()
            if(Build.VERSION.SDK_INT>=31) {
                try { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) } catch (_:Exception) {}
            }
        },lp(-1,-2,0,16,0,0))
        root.addView(text("ملاحظة: يعتمد وصول التنبيه على سماح النظام بالإشعارات والمنبّهات الدقيقة.",11f,muted),lp(-1,-2,0,10,0,0))
    }

    private fun pickDate(dateButtonRef: View?) {
        DatePickerDialog(this,{_,y,m,d->selectedDate.set(y,m,d);render()},selectedDate.get(Calendar.YEAR),selectedDate.get(Calendar.MONTH),selectedDate.get(Calendar.DAY_OF_MONTH)).show()
    }
    private fun dateFmt(d: Date)=SimpleDateFormat("yyyy/MM/dd",Locale.getDefault()).format(d)
    private fun dateTimeFmt(ms:Long)=SimpleDateFormat("EEE d MMM • HH:mm",Locale.forLanguageTag("ar")).format(Date(ms))
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun lp(w:Int,h:Int,l:Int=0,t:Int=0,r:Int=0,b:Int=0)=LinearLayout.LayoutParams(if(w>0)dp(w) else w,if(h>0)dp(h) else h).apply{setMargins(dp(l),dp(t),dp(r),dp(b))}
}
