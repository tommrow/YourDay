package com.wuxiao.yourday.diary;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationListener;
import com.wuxiao.yourday.R;
import com.wuxiao.yourday.base.BaseFragment;
import com.wuxiao.yourday.bean.DiaryTime;
import com.wuxiao.yourday.bean.Note;
import com.wuxiao.yourday.bean.WeatherItem;
import com.wuxiao.yourday.common.AMapLocationManager;
import com.wuxiao.yourday.common.RichEditText.EditTextData;
import com.wuxiao.yourday.common.RichEditText.RichEditText;
import com.wuxiao.yourday.common.ThemeManager;
import com.wuxiao.yourday.common.TimeUtils;
import com.wuxiao.yourday.common.photo.PhotoPickerActivity;
import com.wuxiao.yourday.common.popup.WeatherCallBack;
import com.wuxiao.yourday.common.popup.WeatherPopup;
import com.wuxiao.yourday.databinding.FragmentDiaryBinding;
import com.wuxiao.yourday.viewpager.FragmentVisibilityListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;

import static com.wuxiao.yourday.common.popup.WeatherPopup.getMenu;


/**
 * Created by wuxiaojian on 16/12/6.
 */
public class DiaryFragment extends BaseFragment<DiaryPresenter> implements DiaryContract.DiaryView, DatePickerDialog.OnDateSetListener, View.OnClickListener
        , TimePickerDialog.OnTimeSetListener, WeatherCallBack, AMapLocationListener, FragmentVisibilityListener {
    private Calendar calendar;
    private TimeUtils timeUtils;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private LinearLayout buttom_toolbar;
    private ImageView photo;
    private ImageView location;
    private ImageView save;
    private ImageView clear;
    private RichEditText note_rich;
    public static final int REQUEST_PHOTO = 0x0023;
    private List<WeatherItem> weatherList;
    private WeatherPopup weatherPopup;
    private int weatherPisition;
    private AMapLocationClient client;
    private TextView diary_location;
    private StringBuilder location1;
    private FragmentDiaryBinding diaryBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        calendar = Calendar.getInstance();
        timeUtils = TimeUtils.getInstance(getActivity().getApplicationContext());
        weatherList = getMenu(getActivity());
        weatherPopup = new WeatherPopup(getActivity(), this);
        client = AMapLocationManager.getInstance(getActivity().getApplication());
        client.setLocationListener(this);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        diaryBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_diary, container, false);
        diaryBinding.setCalendarColor(ThemeManager.getInstance().getThemeColor(getActivity()));
        diaryBinding.setWeatherPosition(0);
        note_rich = diaryBinding.noteRich;
        diary_location = diaryBinding.diaryLocation;
        buttom_toolbar = (LinearLayout) diaryBinding.getRoot().findViewById(R.id.buttom_toolbar);
        buttom_toolbar.setBackgroundColor(ThemeManager.getInstance().getThemeColor(getActivity()));
        diary_location = (TextView) diaryBinding.getRoot().findViewById(R.id.diary_location);
        diaryBinding.getRoot().findViewById(R.id.save).setOnClickListener(this);
        diaryBinding.diaryTimeInformation.setOnClickListener(this);
        diaryBinding.weatherIcon.setOnClickListener(this);
        diaryBinding.noteRichLinear.setOnClickListener(this);
        photo = (ImageView) diaryBinding.getRoot().findViewById(R.id.photo);
        photo.setVisibility(View.VISIBLE);
        photo.setOnClickListener(this);
        location = (ImageView) diaryBinding.getRoot().findViewById(R.id.location);
        location.setVisibility(View.VISIBLE);
        location.setOnClickListener(this);
        save = (ImageView) diaryBinding.getRoot().findViewById(R.id.save);
        save.setVisibility(View.VISIBLE);
        save.setOnClickListener(this);
        clear = (ImageView) diaryBinding.getRoot().findViewById(R.id.clear);
        clear.setVisibility(View.VISIBLE);
        clear.setOnClickListener(this);
        client.startLocation();
        return diaryBinding.getRoot();
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCurrentTime(true);
    }


    public static DiaryFragment newInstance() {
        DiaryFragment fragment = new DiaryFragment();
        return fragment;
    }

    private void setCurrentTime(boolean updateCurrentTime) {
        if (updateCurrentTime) {
            calendar.setTimeInMillis(System.currentTimeMillis());
        }
        DiaryTime diaryTime = new DiaryTime();
        diaryTime.setMonth(timeUtils.getMonth()[calendar.get(Calendar.MONTH)]);
        diaryTime.setTime(sdf.format(calendar.getTime()));
        diaryTime.setDate(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
        diaryTime.setDay(timeUtils.getDays()[calendar.get(Calendar.DAY_OF_WEEK) - 1]);
        diaryBinding.setDiaryTime(diaryTime);

    }


    @Override
    protected DiaryPresenter getPresenter() {
        return new DiaryPresenter(getActivity(), this);
    }


    @Override
    public void loadView(Throwable e) {

    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        if (view.isShown()) {
            calendar.set(year, monthOfYear, dayOfMonth);
            setCurrentTime(false);
            TimePickerFragment timePickerFragment = TimePickerFragment.newInstance(calendar.getTimeInMillis());
            timePickerFragment.setOnTimeSetListener(this);
            timePickerFragment.show(getFragmentManager(), "timePickerFragment");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.diary_time_information:
                DatePickerFragment datePickerFragment = DatePickerFragment.newInstance(calendar.getTimeInMillis());
                datePickerFragment.setOnDateSetListener(this);
                datePickerFragment.show(getFragmentManager(), "datePickerFragment");
                break;
            case R.id.save:
                List<EditTextData> editList = note_rich.GetEditData();


                final StringBuilder content = new StringBuilder();

                Observable.from(editList).subscribe(new Action1<EditTextData>() {
                    @Override
                    public void call(EditTextData data) {
                        if (data.getInputStr() != null) {

                            content.append(data.getInputStr()).append("*");
                        } else if (data.getImagePath() != null) {

                            content.append(data.getImagePath()).append("*");
                        }
                    }
                });
                String title = note_rich.getTitleData();
                long createTime = calendar.getTimeInMillis();
                if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(content)) {
                    if (location1.toString() != null) {
                        mPresenter.insertNote(title, content.toString(), createTime, weatherPisition, location1.toString());
                    } else {
                        mPresenter.insertNote(title, content.toString(), createTime, weatherPisition, "北京");

                    }
                    weatherPisition = 0;
                    diaryBinding.setWeatherPosition(0);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.diary_empty), Toast.LENGTH_SHORT).show();
                }


                break;
            case R.id.photo:
                startActivityForResult(new Intent(getActivity(), PhotoPickerActivity.class), REQUEST_PHOTO);
                break;
            case R.id.location:

                break;
            case R.id.clear:
                diaryBinding.setWeatherPosition(0);
                saveStatus();
                break;
            case R.id.weather_icon:
                weatherPopup.showPopupWindow();
                break;
            case R.id.note_rich_linear:
                note_rich.showMethodManager();
                break;
            default:
                break;
        }
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if (view.isShown()) {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            setCurrentTime(false);
        }
    }

    @Override
    public void saveStatus() {
        setCurrentTime(true);
        note_rich.setTextTitleHint();
    }

    @Override
    public void responseNoteDetail(Note note) {

    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != getActivity().RESULT_OK) return;
        if (requestCode == REQUEST_PHOTO) {
            String[] photoPaths = data.getStringArrayExtra(PhotoPickerActivity.INTENT_PHOTO_PATHS);
            note_rich.addImageArray(photoPaths);
        }
    }


    @Override
    public void weatherPosition(int position) {
        weatherPisition = position;
        diaryBinding.setWeatherPosition(position);
        weatherPopup.dismiss();
    }


    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (null != aMapLocation) {
            location1 = new StringBuilder();
            location1.append(aMapLocation.getCity());
            location1.append(aMapLocation.getDistrict());
            diary_location.setText(location1.toString());
        }
    }

    @Override
    public void onFragmentVisible() {
        if (location1 != null) {
            diary_location.setText(location1.toString());
        } else {
            diary_location.setText("正在定位");
        }
    }

    @Override
    public void onFragmentInvisible() {
        note_rich.hideMethodManager();
    }

}
