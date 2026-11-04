// 선 아이콘 모음. 크기와 색은 부모의 font-size·color를 따르지 않고 size·currentColor로 정한다.

function Svg({ size = 24, strokeWidth = 2, children, ...rest }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={strokeWidth}
         strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false" {...rest}>
      {children}
    </svg>
  );
}

export const HomeIcon = (p) => (
  <Svg {...p}><path d="M3 10.5 12 3l9 7.5" /><path d="M5 9.5V20h14V9.5" /><path d="M10 20v-5h4v5" /></Svg>
);
export const ListIcon = (p) => (
  <Svg {...p}><path d="M9 6h11M9 12h11M9 18h11" /><circle cx="4.5" cy="6" r="1" /><circle cx="4.5" cy="12" r="1" /><circle cx="4.5" cy="18" r="1" /></Svg>
);
export const ChartIcon = (p) => (
  <Svg {...p}><path d="M4 20h16" /><rect x="5" y="11" width="3.5" height="6" rx="1" /><rect x="10.25" y="6" width="3.5" height="11" rx="1" /><rect x="15.5" y="13" width="3.5" height="4" rx="1" /></Svg>
);
export const UserIcon = (p) => (
  <Svg {...p}><circle cx="12" cy="8" r="4" /><path d="M4 21c1.5-4 4.5-6 8-6s6.5 2 8 6" /></Svg>
);
export const BellIcon = (p) => (
  <Svg {...p}><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" /><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" /></Svg>
);
export const PlusIcon = (p) => (
  <Svg strokeWidth={2.6} {...p}><path d="M12 5v14M5 12h14" /></Svg>
);
export const BackIcon = (p) => (
  <Svg strokeWidth={2.4} {...p}><path d="M15 18l-6-6 6-6" /></Svg>
);
export const ChevronIcon = (p) => (
  <Svg size={18} {...p}><path d="M9 18l6-6-6-6" /></Svg>
);
export const ChevronLeftIcon = (p) => (
  <Svg size={20} {...p}><path d="M15 18l-6-6 6-6" /></Svg>
);
export const CalendarIcon = (p) => (
  <Svg {...p}><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M3 10h18M8 3v4M16 3v4" /><path d="M9 15l2 2 4-4" /></Svg>
);
export const LogoutIcon = (p) => (
  <Svg {...p}><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><path d="M16 17l5-5-5-5M21 12H9" /></Svg>
);
export const ShieldIcon = (p) => (
  <Svg {...p}><path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z" /></Svg>
);
