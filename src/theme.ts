import { createTheme } from '@mui/material/styles';

export type ColorMode = 'light' | 'dark';

export const theme = createTheme({
  cssVariables: { colorSchemeSelector: 'data-theme' },
  colorSchemes: {
    light: {
      palette: {
        primary: { main: '#315d46', dark: '#244734', light: '#e7efdf' },
        secondary: { main: '#b27b32' },
        background: { default: '#f3f2ec', paper: '#fffefa' },
        text: { primary: '#1c2b23', secondary: '#66746b' },
        divider: '#e1e4d9',
        success: { main: '#47784e' },
        warning: { main: '#aa7623' },
        error: { main: '#bf5545' },
      },
    },
    dark: {
      palette: {
        primary: { main: '#b7d77d', dark: '#c9e694', light: '#2b3b2b' },
        secondary: { main: '#edc677' },
        background: { default: '#121a16', paper: '#1b2620' },
        text: { primary: '#eff1e7', secondary: '#a9b5a8' },
        divider: '#34443a',
        success: { main: '#8ebc7e' },
        warning: { main: '#e2bd6d' },
        error: { main: '#e38470' },
      },
    },
  },
  defaultColorScheme: 'light',
  shape: { borderRadius: 16 },
  typography: {
    fontFamily: '"Inter", "Segoe UI", Arial, sans-serif',
    h4: { fontWeight: 760, letterSpacing: '-0.045em', lineHeight: 1.1 },
    h5: { fontWeight: 740, letterSpacing: '-0.035em', lineHeight: 1.2 },
    h6: { fontWeight: 720, letterSpacing: '-0.025em', lineHeight: 1.25 },
    body1: { lineHeight: 1.6 },
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
          borderRadius: 20,
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
