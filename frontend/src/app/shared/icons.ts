import { LUCIDE_ICONS, LucideIconProvider } from 'lucide-angular';
import {
  ArrowLeft, ArrowRight, Ban, Banknote, Beef, Bike, CakeSlice, ChartColumn, Check, ChefHat, ChevronDown,
  ChevronLeft, ChevronRight, CircleAlert, CircleCheck, ClipboardList, Clock, CreditCard, CupSoda, Drumstick,
  Eye, EyeOff, Flame, Grid2x2, House, Image, Inbox, Info, Layers, LayoutDashboard, Leaf, ListFilter, Loader,
  Lock, LogOut, Mail, MapPin, Menu, Minus, Package, PackageCheck, Pencil, Percent, Phone, Pizza, Plus, Popcorn,
  Printer, Receipt, RefreshCw, Sandwich, Search, Settings, ShoppingBag, SlidersHorizontal, Soup, Sparkles, Star,
  Store, Tag, Ticket, Timer, Trash2, TrendingUp, TriangleAlert, Truck, Undo2, Upload, User, Users, Utensils,
  Wallet, Wheat, X,
} from 'lucide-angular';

/** Every icon the app uses, registered once so templates can use `<lucide-icon name="pizza">`. */
export const ICON_PROVIDER = {
  provide: LUCIDE_ICONS,
  multi: true,
  useValue: new LucideIconProvider({
    ArrowLeft, ArrowRight, Ban, Banknote, Beef, Bike, CakeSlice, ChartColumn, Check, ChefHat, ChevronDown,
    ChevronLeft, ChevronRight, CircleAlert, CircleCheck, ClipboardList, Clock, CreditCard, CupSoda, Drumstick,
    Eye, EyeOff, Flame, Grid2x2, House, Image, Inbox, Info, Layers, LayoutDashboard, Leaf, ListFilter, Loader,
    Lock, LogOut, Mail, MapPin, Menu, Minus, Package, PackageCheck, Pencil, Percent, Phone, Pizza, Plus, Popcorn,
    Printer, Receipt, RefreshCw, Sandwich, Search, Settings, ShoppingBag, SlidersHorizontal, Soup, Sparkles, Star,
    Store, Tag, Ticket, Timer, Trash2, TrendingUp, TriangleAlert, Truck, Undo2, Upload, User, Users, Utensils,
    Wallet, Wheat, X,
  }),
};
