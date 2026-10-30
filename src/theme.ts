import { createTheme } from '@mui/material/styles';

export type ColorMode = 'light' | 'dark';

export const theme = createTheme({
  cssVariables: { colorSchemeSelector: 'data-theme' },
  colorSchemes: {
    light: {
      palette: {
        primary: { main: '#397760', dark: '#275b46', light: '#e7f2eb' },
        secondary: { main: '#b67a46' },
        background: { default: '#f3f6f4', paper: '#ffffff' },
        text: { primary: '#1d2b25', secondary: '#65756d' },
        divider: '#e1e9e3',
        success: { main: '#31835e' },
        warning: { main: '#b8782b' },
        error: { main: '#c2534b' },
      },
    },
    dark: {
      palette: {
        primary: { main: '#82c29a', dark: '#a3d8b5', light: '#263d30' },
        secondary: { main: '#e0ad78' },
        background: { default: '#121b17', paper: '#1a2620' },
        text: { primary: '#ecf4ee', secondary: '#a2b3a8' },
        divider: '#304238',
        success: { main: '#75c796' },
        warning: { main: '#e0ad65' },
        error: { main: '#e77b70' },
      },
    },
  },
  defaultColorScheme: 'light',
  shape: { borderRadius: 16 },
  typography: {
    fontFamily: '"Inter", "Segoe UI", Arial, sans-serif',
    h4: { fontWeight: 750, letterSpacing: '-0.04em' },
    h5: { fontWeight: 700, letterSpacing: '-0.03em' },
    h6: { fontWeight: 700, letterSpacing: '-0.02em' },
    body1: { lineHeight: 1.55 },
    body2: { lineHeight: 1.55 },
    button: { fontWeight: 650, textTransform: 'none' },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          border: '1px solid var(--mui-palette-divider)',
          boxShadow: 'var(--app-card-shadow)',
          backgroundImage: 'none',
          borderRadius: 18,
        },
      },
    },
    MuiButton: { styleOverrides: { root: { borderRadius: 12, fontWeight: 700, transition: 'background-color 140ms ease, border-color 140ms ease, box-shadow 140ms ease, transform 140ms ease' } } },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          backgroundColor: 'var(--surface-raised)',
          '& .MuiOutlinedInput-notchedOutline': { borderColor: 'var(--mui-palette-divider)' },
          '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: 'var(--mui-palette-primary-main)' },
          '&.Mui-focused': { boxShadow: '0 0 0 3px color-mix(in srgb, var(--mui-palette-primary-main) 15%, transparent)' },
        },
      },
    },
    MuiPaper: { styleOverrides: { root: { backgroundImage: 'none' } } },
    MuiAppBar: { styleOverrides: { root: { backgroundImage: 'none' } } },
    MuiTableCell: {
      styleOverrides: {
        root: { borderColor: 'var(--app-border-soft)' },
        head: { color: 'var(--mui-palette-text-secondary)', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase' },
      },
    },
  },
});
