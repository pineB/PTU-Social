import { createTheme } from '@mui/material/styles';

export type ColorMode = 'light' | 'dark';

export const theme = createTheme({
  cssVariables: { colorSchemeSelector: 'data-theme' },
  colorSchemes: {
    light: {
      palette: {
        primary: { main: '#6558e8', dark: '#4e43c4', light: '#eeecff' },
        secondary: { main: '#168f76' },
        background: { default: '#f5f6fb', paper: '#ffffff' },
        text: { primary: '#20243a', secondary: '#777d92' },
        divider: '#e8eaf2',
        success: { main: '#159b79' },
        warning: { main: '#de9a35' },
        error: { main: '#d45768' },
      },
    },
    dark: {
      palette: {
        primary: { main: '#a39aff', dark: '#8678f4', light: '#d2ccff' },
        secondary: { main: '#55d6b1' },
        background: { default: '#10121a', paper: '#191c27' },
        text: { primary: '#f0f0f8', secondary: '#a1a5b8' },
        divider: '#2b2e3b',
        success: { main: '#58c8a4' },
        warning: { main: '#f0b968' },
        error: { main: '#f17b8a' },
      },
    },
  },
  defaultColorScheme: 'light',
  shape: { borderRadius: 14 },
  typography: {
    fontFamily: '"Inter", "Segoe UI", Arial, sans-serif',
    h4: { fontWeight: 750, letterSpacing: '-0.04em' },
    h5: { fontWeight: 700, letterSpacing: '-0.03em' },
    h6: { fontWeight: 700, letterSpacing: '-0.02em' },
    button: { fontWeight: 650, textTransform: 'none' },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          border: '1px solid var(--mui-palette-divider)',
          boxShadow: 'var(--app-card-shadow)',
          backgroundImage: 'none',
        },
      },
    },
    MuiButton: { styleOverrides: { root: { borderRadius: 10 } } },
    MuiAppBar: { styleOverrides: { root: { backgroundImage: 'none' } } },
    MuiTableCell: {
      styleOverrides: {
        root: { borderColor: 'var(--app-border-soft)' },
        head: { color: 'var(--mui-palette-text-secondary)', fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase' },
      },
    },
  },
});
